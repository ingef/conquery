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
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.FullExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.execution.OverviewExecutionStatus;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.specific.CancelQuery;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryTranslator;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.SecondaryIdQuery;
import com.bakdata.conquery.models.query.concept.specific.CQAnd;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
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
	 * @return
	 */
	public ManagedExecution<?> postQuery(Dataset dataset, QueryDescription query, User user) {

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

			final ManagedExecution<?> execution = tryReuse(query, executionId, datasetRegistry, config);

			if (execution != null) {
				return execution;
			}
		}

		// Run the query on behalf of the user
		ManagedExecution<?> mq = ExecutionManager.runQuery(datasetRegistry, query, user, dataset, config);

		if (query instanceof IQuery) {
			translateToOtherDatasets(dataset, query, user, mq);
		}

		// return status
		return mq;
	}

	private ManagedExecution<?> tryReuse(QueryDescription query, Optional<ManagedExecutionId> maybeId, DatasetRegistry datasetRegistry, ConqueryConfig config) {

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

		ExecutionState state = execution.getState();
		if (state.equals(ExecutionState.RUNNING)) {
			log.trace("The Execution[{}] was already started and its state is: {}", execution.getId(), state);
			return execution;
		}

		log.trace("Re-executing Query {}", execution);

		ExecutionManager.execute(datasetRegistry, execution, config);

		return execution;

	}


	public Stream<ExecutionStatus> getAllQueries(Namespace namespace, HttpServletRequest req, User user, boolean allProviders) {
		Collection<ManagedExecution<?>> allQueries = storage.getAllExecutions();

		return getQueriesFiltered(namespace.getDataset(), RequestAwareUriBuilder.fromRequest(req), user, allQueries, allProviders);
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
					if (mq.isReadyToDownload(datasetAbilities)){
						setDownloadUrls(status, config.getResultProviders(), mq, uriBuilder, allProviders);
					}
					return status;
				});
	}



	/**
	 * Sets the result urls for the given result renderer. Result urls are only rendered for providers that match the
	 * result type of the execution.
	 * @param status The status that is edited.
	 * @param renderer The renderer that are requested for a result url generation.
	 * @param exec The execution that is used for generating the url
	 * @param uriBuilder The Uribuilder with the base configuration to generate the urls
	 * @param allProviders If true, the {@link ResultRendererProvider#isHidden()} is ignored and a result urls is generated
	 *                     anyways
	 * @param <S> The type of the provided and returned status
	 * @return The modified status
	 */
	public static <S extends ExecutionStatus> S setDownloadUrls(S status, List<ResultRendererProvider> renderer, ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders){

		List<URL> resultUrls = renderer.stream()
				.map(r -> r.generateResultURL(exec,uriBuilder.clone(), allProviders))
				.flatMap(Optional::stream).collect(Collectors.toList());

		status.setResultUrls(resultUrls);

		return status;
	}


	private static boolean canFrontendRender(ManagedExecution<?> q) {
		if (!(q instanceof ManagedQuery)) {
			return false;
		}

		final IQuery query = ((ManagedQuery) q).getQuery();

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
		IQuery translateable = (IQuery) query;
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

				IQuery translated = QueryTranslator.replaceDataset(datasetRegistry, translateable, targetDataset);
				ExecutionManager.createQuery(datasetRegistry, translated, mq.getQueryId(), user, targetDataset);
			}
			catch (Exception e) {
				log.trace("Could not translate " + query + " to dataset " + targetDataset, e);
			}
		}
	}

	public void cancel(User user, Dataset dataset, ManagedExecution<?> query) {

		log.debug("{} cancelled Query[{}]", user, query.getId());

		final Namespace namespace = getDatasetRegistry().get(dataset.getId());

		namespace.sendToAll(new CancelQuery(query.getId()));

		query.setState(ExecutionState.CANCELED);
	}



	public void patchQuery(User user, ManagedExecution<?> execution, MetaDataPatch patch) throws JSONException {

		log.trace("Patching {} ({}) with patch: {}", execution.getClass().getSimpleName(), execution, patch);
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

	public void reexecute(ManagedExecution<?> query) {
		if(!query.getState().equals(ExecutionState.RUNNING)) {
			ExecutionManager.execute(getDatasetRegistry(), query, config);
		}
	}


	public void deleteQuery(ManagedExecution<?> execution) {

		storage.removeExecution(execution.getId());
	}

	public FullExecutionStatus getQueryFullStatus(ManagedExecution<?> query, User user, UriBuilder url, Boolean allProviders) {

		query.initExecutable(datasetRegistry, config);

		Map<DatasetId, Set<Ability>> datasetAbilities = buildDatasetAbilityMap(user, datasetRegistry);
		final FullExecutionStatus status = query.buildStatusFull(storage, url, user, datasetRegistry, datasetAbilities);

		if (query.isReadyToDownload(datasetAbilities)){
			setDownloadUrls(status, config.getResultProviders(), query, url, allProviders);
		}
		return status;
	}


}
