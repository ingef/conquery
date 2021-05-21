package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.FullExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryTranslator;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.SecondaryIdQuery;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.bakdata.conquery.apiv1.StoredQueriesProcessor.setDownloadUrls;

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
	public FullExecutionStatus postQuery(Dataset dataset, QueryDescription query, UriBuilder urlb, User user) {
		user.authorize(dataset, Ability.READ);

		// This maps works as long as we have query visitors that are not configured in anyway.
		// So adding a visitor twice would replace the previous one but both would have yielded the same result.
		// For the future a better data structure might be desired that also regards similar QueryVisitors of different configuration
		ClassToInstanceMap<QueryVisitor> visitors = MutableClassToInstanceMap.create();
		query.addVisitors(visitors);

		// Initialize checks that need to traverse the query tree
		visitors.putInstance(QueryUtils.OnlyReusingChecker.class, new QueryUtils.OnlyReusingChecker());
		visitors.putInstance(NamespacedIdentifiableCollector.class, new NamespacedIdentifiableCollector());

		final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(user, storage).map(Group::getName).orElse("none");

		visitors.putInstance(ExecutionMetrics.QueryMetricsReporter.class, new ExecutionMetrics.QueryMetricsReporter(primaryGroupName));


		// Chain all Consumers
		Consumer<Visitable> consumerChain = QueryUtils.getNoOpEntryPoint();
		for (QueryVisitor visitor : visitors.values()) {
			consumerChain = consumerChain.andThen(visitor);
		}

		// Apply consumers to the query tree
		query.visit(consumerChain);


		query.authorize(user, dataset, visitors);

		ExecutionMetrics.reportNamespacedIds(visitors.getInstance(NamespacedIdentifiableCollector.class).getIdentifiables(), primaryGroupName);

		ExecutionMetrics.reportQueryClassUsage(query.getClass(), primaryGroupName);


		// If this is only a re-executing query, try to execute the underlying query instead.
		{
			final Optional<ManagedExecutionId> executionId = visitors.getInstance(QueryUtils.OnlyReusingChecker.class).getOnlyReused();

			final FullExecutionStatus status = tryReuse(query, executionId, user, datasetRegistry, config, urlb);

			if (status != null) {
				return status;
			}
		}

		// Run the query on behalf of the user
		ManagedExecution<?> mq = ExecutionManager.runQuery(datasetRegistry, query, user, dataset, config);

		if (query instanceof IQuery) {
			translateToOtherDatasets(dataset, query, user, mq);
		}

		// return status
		return getStatus(mq, urlb, user);
	}

	private FullExecutionStatus tryReuse(QueryDescription query, Optional<ManagedExecutionId> maybeId, User user, DatasetRegistry datasetRegistry, ConqueryConfig config, UriBuilder urlb) {

		// If this is only a re-executing query, execute the underlying query instead.
		if (maybeId.isEmpty()) {
			return null;
		}

		final ManagedExecution<?> execution = maybeId.map(datasetRegistry.getMetaRegistry()::resolve).orElse(null);

		if(execution == null){
			return null;
		}

		// Direct reuse only works if the queries are of the same type (As reuse reconstructs the Query for different types)
		if (!query.getClass().equals(execution.getSubmitted().getClass())) {
			return null;
		}

		// If SecondaryIds differ from selected and prior, we cannot reuse them.
		if (query instanceof SecondaryIdQuery) {
			final SecondaryIdDescription selectedSecondaryId = ((SecondaryIdQuery) query).getSecondaryId();
			final SecondaryIdDescription reusedSecondaryId = ((SecondaryIdQuery) execution.getSubmitted()).getSecondaryId();

			if (!selectedSecondaryId.equals(reusedSecondaryId)) {
				return null;
			}
		}

		FullExecutionStatus status = getStatus(execution, urlb, user);

		ExecutionState state = status.getStatus();
		if (state.equals(ExecutionState.RUNNING)) {
			log.trace("The Execution[{}] was already started and its state is: {}", execution.getId(), state);
			return status;
		}

		log.trace("Re-executing Query {}", execution);

		ExecutionManager.execute(datasetRegistry, execution, config);

		return getStatus(execution, urlb, user);

	}

	private void translateToOtherDatasets(Dataset dataset, QueryDescription query, User user, ManagedExecution<?> mq) {
		IQuery translateable = (IQuery) query;
		// translate the query for all other datasets of user and submit it.
		for (Namespace targetNamespace : datasetRegistry.getDatasets()) {
			final Dataset targetDataset = targetNamespace.getDataset();
			if (targetDataset.equals(dataset)) {
				continue;
			}

			if (user.isPermitted(targetDataset, Ability.READ)) {
				continue;
			}

			try {

				IQuery translated = QueryTranslator.replaceDataset(datasetRegistry, translateable, targetDataset);
				ExecutionManager.createQuery(datasetRegistry, translated, mq.getQueryId(), user, targetDataset);
			}
			catch (Exception e) {
				log.trace("Could not translate " + query + " to dataset " + targetDataset, e);
			}
		}
	}

	public FullExecutionStatus getStatus(ManagedExecution<?> query, UriBuilder urlb, User user) {
		query.initExecutable(datasetRegistry, config);
		final Map<DatasetId, Set<Ability>> datasetAbilities = AuthorizationHelper.buildDatasetAbilityMap(user, datasetRegistry);
		final FullExecutionStatus status = query.buildStatusFull(storage, urlb, user, datasetRegistry, datasetAbilities);
		if (query.isReadyToDownload(datasetAbilities)){
			setDownloadUrls(status, config.getResultProviders(), query, urlb);
		}
		return status;
	}

	public FullExecutionStatus cancel(User user, Dataset dataset, ManagedExecution<?> query, UriBuilder urlb) {
		// TODO implement query cancel functionality
		user.authorize(query, Ability.CANCEL);
		return null;
	}


}
