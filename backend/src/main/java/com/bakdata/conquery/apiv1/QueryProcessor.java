package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.QueryTranslator;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;
@Slf4j
@RequiredArgsConstructor
public class QueryProcessor {

	@Getter
	private final Namespaces namespaces;
	private final MasterMetaStorage storage;
	
	private static final Consumer<Visitable> NOOP = whatever -> {};

	/**
	 * Creates a query for all datasets, then submits it for execution on the
	 * intended dataset.
	 */
	public ExecutionStatus postQuery(Dataset dataset, QueryDescription query, URLBuilder urlb, User user) {
		authorize(user, dataset.getId(), Ability.READ);
		
		// Initialize the query
		query = query.resolve(new QueryResolveContext(dataset.getId(), namespaces));
		
		// This maps works as long as we have query visitors that are not configured in anyway.
		// So adding a visitor twice would replace the previous one but both would have yielded the same result.
		// For the future a better data structure might be desired that also regards similar QueryVisitors of different configuration
		ClassToInstanceMap<QueryVisitor> visitors = MutableClassToInstanceMap.create();
		query.addVisitors(visitors);
		
		// Initialize checks that need to traverse the query tree
		visitors.putInstance(QueryUtils.ExternalIdChecker.class, new QueryUtils.ExternalIdChecker());
		visitors.putInstance(QueryUtils.SingleReusedChecker.class, new QueryUtils.SingleReusedChecker());
		visitors.putInstance(QueryUtils.NamespacedIdCollector.class, new QueryUtils.NamespacedIdCollector());
		visitors.putInstance(ExecutionMetrics.QueryMetricsReporter.class, new ExecutionMetrics.QueryMetricsReporter());
		
		
		// Chain all Consumers
		Consumer<Visitable> consumerChain = NOOP;
		for(QueryVisitor visitor : visitors.values()) {
			consumerChain = consumerChain.andThen(visitor);
		}

		// Apply consumers to the query tree
		query.visit(consumerChain);

		
		Set<Permission> permissions = new HashSet<>();
		query.collectPermissions(visitors, permissions);
		user.checkPermissions(permissions);

		ExecutionMetrics.reportNamespacedIds(visitors.getInstance(NamespacedIdCollector.class).getIds(), user, storage);


		ExecutionMetrics.reportQueryClassUsage(query.getClass());


		// Evaluate the checks and take action
		{
			// If this is only a re-executing query, execute the underlying query instead.
			final ManagedExecutionId executionId = visitors.getInstance(QueryUtils.SingleReusedChecker.class).getOnlyReused();

			if (executionId != null) {
				log.info("Re-executing Query {}", executionId);


				final ManagedExecution<?> mq = ExecutionManager.execute( namespaces, storage.getExecution(executionId));

				return getStatus(dataset, mq, urlb, user);
			}
			
			// Check if the query contains parts that require to resolve external ids. If so the user must have the preserve_id permission on the dataset.
			if(visitors.getInstance(QueryUtils.ExternalIdChecker.class).resolvesExternalIds()) {
				user.checkPermission(DatasetPermission.onInstance(Ability.PRESERVE_ID, dataset.getId()));
			}
		}
		
		// Run the query on behalf of the user
		ManagedExecution<?> mq = ExecutionManager.runQuery(namespaces, query, user.getId(), dataset.getId());
		
		// Set abilities for submitted query
		user.addPermission(storage, QueryPermission.onInstance(AbilitySets.QUERY_CREATOR, mq.getId()));

		if(query instanceof IQuery) {
			translateToOtherDatasets(dataset, query, user, mq);
		}

		// return status
		return getStatus(dataset, mq, urlb, user);
	}

	private void translateToOtherDatasets(Dataset dataset, QueryDescription query, User user, ManagedExecution<?> mq) {
		IQuery translateable = (IQuery) query;
		// translate the query for all other datasets of user and submit it.
		for (Namespace targetNamespace : namespaces.getNamespaces()) {
			if (!user.isPermitted(DatasetPermission.onInstance(Ability.READ.asSet(), targetNamespace.getDataset().getId()))
				|| targetNamespace.getDataset().equals(dataset)) {
				continue;
			}
			
			// Ensure that user is allowed to read all sub-queries of the actual query.
			
			if (!translateable.collectRequiredQueries().stream()
				.allMatch(qid -> user.isPermitted(QueryPermission.onInstance(Ability.READ.asSet(), qid)))) {
				continue;				
			}
			
			try {
				DatasetId targetDataset = targetNamespace.getDataset().getId();
				IQuery translated = QueryTranslator.replaceDataset(namespaces, translateable, targetDataset);
				final ManagedExecution<?> mqTranslated = ExecutionManager.createQuery(namespaces, translated, mq.getQueryId(), user.getId(), targetDataset);
				
				user.addPermission(storage, QueryPermission.onInstance(AbilitySets.QUERY_CREATOR, mqTranslated.getId()));
			}
			catch (Exception e) {
				log.trace("Could not translate " + query + " to dataset " + targetNamespace.getDataset(), e);
			}
		}
	}

	public ExecutionStatus getStatus(Dataset dataset, ManagedExecution<?> query, URLBuilder urlb, User user) {
		return query.buildStatus(storage, urlb, user);
	}

	public ExecutionStatus cancel(Dataset dataset, ManagedExecution<?> query, URLBuilder urlb) {
		// TODO implement query cancel functionality
		return null;
	}
}
