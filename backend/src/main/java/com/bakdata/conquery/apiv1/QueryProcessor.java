package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.buildDatasetAbilityMap;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQExternal;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
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
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.specific.CancelQuery;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryTranslator;
import com.bakdata.conquery.models.query.Visitable;
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
	 *
	 * @return
	 */
	public ManagedExecution<?> postQuery(Dataset dataset, QueryDescription query, User user) {

		log.info("Query posted on Dataset[{}] by User[{{}].", dataset.getId(), user.getId());

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

		final Namespace namespace = datasetRegistry.get(dataset.getId());
		final ExecutionManager executionManager = namespace.getExecutionManager();


		// If this is only a re-executing query, try to execute the underlying query instead.
		{
			final Optional<ManagedExecutionId> executionId = visitors.getInstance(QueryUtils.OnlyReusingChecker.class).getOnlyReused();

			final Optional<ManagedExecution<?>> execution = executionId.map(id -> tryReuse(query, id, datasetRegistry, config, executionManager));

			if (execution.isPresent()) {
				return execution.get();
			}
		}


		// Run the query on behalf of the user
		ManagedExecution<?> mq = executionManager.runQuery(datasetRegistry, query, user, dataset, config);

		if (query instanceof Query) {
			translateToOtherDatasets(dataset, query, user, mq);
		}

		// return status
		return mq;
	}

	/**
	 * Determine if the submitted query does reuse ONLY another query and restart that instead of creating another one.
	 */
	private ManagedExecution<?> tryReuse(QueryDescription query, ManagedExecutionId executionId, DatasetRegistry datasetRegistry, ConqueryConfig config, ExecutionManager executionManager) {

		final ManagedExecution<?> execution = datasetRegistry.getMetaRegistry().resolve(executionId);

		if (execution == null) {
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

		ExecutionState state = execution.getState();
		if (state.equals(ExecutionState.RUNNING)) {
			log.trace("The Execution[{}] was already started and its state is: {}", execution.getId(), state);
			return execution;
		}

		log.trace("Re-executing Query {}", execution);

		executionManager.execute(datasetRegistry, execution, config);

		return execution;

	}


	public Stream<ExecutionStatus> getAllQueries(Dataset dataset, HttpServletRequest req, User user, boolean allProviders) {
		Collection<ManagedExecution<?>> allQueries = storage.getAllExecutions();

		return getQueriesFiltered(dataset, RequestAwareUriBuilder.fromRequest(req), user, allQueries, allProviders);
	}

	public Stream<ExecutionStatus> getQueriesFiltered(Dataset datasetId, UriBuilder uriBuilder, User user, Collection<ManagedExecution<?>> allQueries, boolean allProviders) {
		Map<DatasetId, Set<Ability>> datasetAbilities = buildDatasetAbilityMap(user, datasetRegistry);

		return allQueries.stream()
						 // The following only checks the dataset, under which the query was submitted, but a query can target more that
						 // one dataset.
						 .filter(q -> q.getDataset().equals(datasetId))
						 // to exclude subtypes from somewhere else
						 .filter(QueryProcessor::canFrontendRender)
						 .filter(q -> q.getState().equals(ExecutionState.DONE) || q.getState().equals(ExecutionState.NEW))
						 // We decide, that if a user owns an execution it is permitted to see it, which saves us a lot of permissions
						 // However, for other executions we check because those are probably shared.
						 .filter(q -> user.isPermitted(q, Ability.READ))
						 .map(mq -> {
							 OverviewExecutionStatus status = mq.buildStatusOverview(
									 uriBuilder.clone(),
									 user,
									 datasetAbilities
							 );
							 if (mq.isReadyToDownload(datasetAbilities)) {
								 setDownloadUrls(status, config.getResultProviders(), mq, uriBuilder, allProviders);
							 }
							 return status;
						 });
	}


	/**
	 * Sets the result urls for the given result renderer. Result urls are only rendered for providers that match the
	 * result type of the execution.
	 *
	 * @param status       The status that is edited.
	 * @param renderer     The renderer that are requested for a result url generation.
	 * @param exec         The execution that is used for generating the url
	 * @param uriBuilder   The Uribuilder with the base configuration to generate the urls
	 * @param allProviders If true, forces {@link ResultRendererProvider} to return an URL if possible.
	 * @param <S>          The type of the provided and returned status
	 * @return The modified status
	 */
	public static <S extends ExecutionStatus> S setDownloadUrls(S status, List<ResultRendererProvider> renderer, ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {

		List<URL> resultUrls = renderer.stream()
									   .map(r -> r.generateResultURL(exec, uriBuilder.clone(), allProviders))
									   .flatMap(Optional::stream).collect(Collectors.toList());

		status.setResultUrls(resultUrls);

		return status;
	}


	/**
	 * Test if the query is structured in a way the Frontend can render it.
	 */
	private static boolean canFrontendRender(ManagedExecution<?> q) {
		if (!(q instanceof ManagedQuery)) {
			return false;
		}

		final Query query = ((ManagedQuery) q).getQuery();

		if (query instanceof ConceptQuery) {
			return isFrontendStructure(((ConceptQuery) query).getRoot());
		}

		if (query instanceof SecondaryIdQuery) {
			return isFrontendStructure(((SecondaryIdQuery) query).getRoot());
		}

		return false;
	}

	/**
	 * Frontend can only render very specific formats properly.
	 *
	 * @implNote We filter for just the bare minimum, as the structure of the frontend is very specific and hard to fix in java code.
	 */
	public static boolean isFrontendStructure(CQElement root) {
		return root instanceof CQAnd || root instanceof CQExternal;
	}

	private void translateToOtherDatasets(Dataset dataset, QueryDescription query, User user, ManagedExecution<?> mq) {
		Query translateable = (Query) query;
		// translate the query for all other datasets of user and submit it.
		for (Namespace targetNamespace : datasetRegistry.getDatasets()) {

			final Dataset targetDataset = targetNamespace.getDataset();

			if (targetDataset.equals(dataset)) {
				continue;
			}

			if (!user.isPermitted(targetDataset, Ability.READ)) {
				continue;
			}

			try {
				log.trace("Adding Query on Dataset[{}]", dataset.getId());
				Query translated = QueryTranslator.replaceDataset(datasetRegistry, translateable, targetDataset);

				targetNamespace.getExecutionManager()
							   .createQuery(datasetRegistry, translated, mq.getQueryId(), user, targetDataset);
			}
			catch (Exception e) {
				log.trace("Could not translate Query[{}] to Dataset[{}]", mq.getId(), targetDataset.getId(), e);
			}
		}
	}

	/**
	 * Cancel a running query: Sending cancellation to shards, which will cause them to stop executing them, results are not sent back, and incoming results will be discarded.
	 */
	public void cancel(User user, Dataset dataset, ManagedExecution<?> query) {

		// Does not make sense to cancel a query that isn't running.
		if (!query.getState().equals(ExecutionState.RUNNING)) {
			return;
		}

		log.info("{} cancelled Query[{}]", user, query.getId());

		final Namespace namespace = getDatasetRegistry().get(dataset.getId());

		query.reset();

		namespace.sendToAll(new CancelQuery(query.getId()));
	}

	public void patchQuery(User user, ManagedExecution<?> execution, MetaDataPatch patch) {

		log.info("Patching {} ({}) with patch: {}", execution.getClass().getSimpleName(), execution, patch);

		patch.applyTo(execution, storage, user);
		storage.updateExecution(execution);

		// Patch this query in other datasets
		List<Dataset> remainingDatasets = datasetRegistry.getAllDatasets();
		remainingDatasets.remove(execution.getDataset());

		for (Dataset dataset : remainingDatasets) {
			ManagedExecutionId id = new ManagedExecutionId(dataset.getId(), execution.getQueryId());
			final ManagedExecution<?> otherExecution = storage.getExecution(id);
			if (otherExecution == null) {
				continue;
			}
			log.trace("Patching {} ({}) with patch: {}", execution.getClass().getSimpleName(), id, patch);
			patch.applyTo(otherExecution, storage, user);
			storage.updateExecution(execution);
		}
	}

	public void reexecute(User user, ManagedExecution<?> query) {
		log.info("User[{}] reexecuted Query[{}]", user, query);

		if (!query.getState().equals(ExecutionState.RUNNING)) {
			datasetRegistry.get(query.getDataset().getId())
						   .getExecutionManager()
						   .execute(getDatasetRegistry(), query, config);
		}
	}


	public void deleteQuery(User user, ManagedExecution<?> execution) {
		log.info("User[{}] deleted Query[{}]", user.getId(), execution.getId());

		execution.getExecutionManager().clearQueryResults(execution);

		storage.removeExecution(execution.getId());
	}

	public FullExecutionStatus getQueryFullStatus(ManagedExecution<?> query, User user, UriBuilder url, Boolean allProviders) {

		query.initExecutable(datasetRegistry, config);

		Map<DatasetId, Set<Ability>> datasetAbilities = buildDatasetAbilityMap(user, datasetRegistry);
		final FullExecutionStatus status = query.buildStatusFull(storage, url, user, datasetRegistry, datasetAbilities);

		if (query.isReadyToDownload(datasetAbilities)) {
			setDownloadUrls(status, config.getResultProviders(), query, url, allProviders);
		}
		return status;
	}


}
