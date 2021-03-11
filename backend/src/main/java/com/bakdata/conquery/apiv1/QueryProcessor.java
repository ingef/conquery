package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryTranslator;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
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
	private final DatasetRegistry datasetRegistry;
	private final MetaStorage storage;
	private final ConqueryConfig config;

	/**
	 * Creates a query for all datasets, then submits it for execution on the
	 * intended dataset.
	 */
	public ExecutionStatus postQuery(Dataset dataset, QueryDescription query, UriBuilder urlb, User user) {
		authorize(user, dataset.getId(), Ability.READ);

		// This maps works as long as we have query visitors that are not configured in anyway.
		// So adding a visitor twice would replace the previous one but both would have yielded the same result.
		// For the future a better data structure might be desired that also regards similar QueryVisitors of different configuration
		ClassToInstanceMap<QueryVisitor> visitors = MutableClassToInstanceMap.create();
		query.addVisitors(visitors);

		// Initialize checks that need to traverse the query tree
		visitors.putInstance(QueryUtils.SingleReusedChecker.class, new QueryUtils.SingleReusedChecker());
		visitors.putInstance(QueryUtils.NamespacedIdCollector.class, new QueryUtils.NamespacedIdCollector());

		final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(user.getId(), storage).map(Group::getName).orElse("none");

		visitors.putInstance(ExecutionMetrics.QueryMetricsReporter.class, new ExecutionMetrics.QueryMetricsReporter(primaryGroupName));


		// Chain all Consumers
		Consumer<Visitable> consumerChain = QueryUtils.getNoOpEntryPoint();
		for (QueryVisitor visitor : visitors.values()) {
			consumerChain = consumerChain.andThen(visitor);
		}

		// Apply consumers to the query tree
		query.visit(consumerChain);


		Set<Permission> permissions = new HashSet<>();
		query.collectPermissions(visitors, permissions, dataset.getId(), storage, user);
		user.checkPermissions(permissions);

		ExecutionMetrics.reportNamespacedIds(visitors.getInstance(NamespacedIdCollector.class).getIds(), primaryGroupName);

		ExecutionMetrics.reportQueryClassUsage(query.getClass(), primaryGroupName);


		// Evaluate the checks and take action
		{
			// If this is only a re-executing query, execute the underlying query instead.
			final ManagedExecutionId executionId = visitors.getInstance(QueryUtils.SingleReusedChecker.class).getOnlyReused();

			if (executionId != null) {
				log.info("Re-executing Query {}", executionId);


				final ManagedExecution<?> mq = ExecutionManager.execute(datasetRegistry, storage.getExecution(executionId), config);

				return getStatus(mq, urlb, user);
			}

		}

		// Run the query on behalf of the user
		ManagedExecution<?> mq = ExecutionManager.runQuery(datasetRegistry, query, user.getId(), dataset.getId(), config);

		if (query instanceof IQuery) {
			translateToOtherDatasets(dataset, query, user, mq);
		}

		// return status
		return getStatus(mq, urlb, user);
	}

	private void translateToOtherDatasets(Dataset dataset, QueryDescription query, User user, ManagedExecution<?> mq) {
		IQuery translateable = (IQuery) query;
		// translate the query for all other datasets of user and submit it.
		for (Namespace targetNamespace : datasetRegistry.getDatasets()) {
			if (!user.isPermitted(DatasetPermission.onInstance(Ability.READ.asSet(), targetNamespace.getDataset().getId()))
					|| targetNamespace.getDataset().equals(dataset)) {
				continue;
			}

			try {
				DatasetId targetDataset = targetNamespace.getDataset().getId();
				IQuery translated = QueryTranslator.replaceDataset(datasetRegistry, translateable, targetDataset);
				final ManagedExecution<?>
						mqTranslated =
						ExecutionManager.createQuery(datasetRegistry, translated, mq.getQueryId(), user.getId(), targetDataset);

			}
			catch (Exception e) {
				log.trace("Could not translate " + query + " to dataset " + targetNamespace.getDataset(), e);
			}
		}
	}

	public ExecutionStatus getStatus(ManagedExecution<?> query, UriBuilder urlb, User user) {
		query.initExecutable(datasetRegistry, config);
		return query.buildStatusFull(storage, urlb, user, datasetRegistry, AuthorizationHelper.buildDatasetAbilityMap(user,datasetRegistry));
	}

	public ExecutionStatus cancel(Dataset dataset, ManagedExecution<?> query, UriBuilder urlb) {
		// TODO implement query cancel functionality
		return null;
	}
}
