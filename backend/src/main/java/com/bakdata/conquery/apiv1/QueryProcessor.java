package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.buildDatasetAbilityMap;

import java.net.URL;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.ExternalUpload;
import com.bakdata.conquery.apiv1.query.ExternalUploadResult;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQUnfilteredTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.specific.CancelQuery;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
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
	 */
	public ManagedExecution<?> postQuery(Dataset dataset, QueryDescription query, Subject subject, boolean system) {

		log.info("Query posted on Dataset[{}] by User[{{}].", dataset.getId(), subject.getId());

		// This maps works as long as we have query visitors that are not configured in anyway.
		// So adding a visitor twice would replace the previous one but both would have yielded the same result.
		// For the future a better data structure might be desired that also regards similar QueryVisitors of different configuration
		ClassToInstanceMap<QueryVisitor> visitors = MutableClassToInstanceMap.create();
		query.addVisitors(visitors);

		// Initialize checks that need to traverse the query tree
		visitors.putInstance(QueryUtils.OnlyReusingChecker.class, new QueryUtils.OnlyReusingChecker());
		visitors.putInstance(NamespacedIdentifiableCollector.class, new NamespacedIdentifiableCollector());

		final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(subject, storage).map(Group::getName).orElse("none");

		visitors.putInstance(ExecutionMetrics.QueryMetricsReporter.class, new ExecutionMetrics.QueryMetricsReporter(primaryGroupName));


		// Chain all Consumers
		Consumer<Visitable> consumerChain = QueryUtils.getNoOpEntryPoint();
		for (QueryVisitor visitor : visitors.values()) {
			consumerChain = consumerChain.andThen(visitor);
		}

		// Apply consumers to the query tree
		query.visit(consumerChain);


		query.authorize(subject, dataset, visitors);
		// After all authorization checks we can now use the actual subject to invoke the query and do not to bubble down the Userish in methods

		ExecutionMetrics.reportNamespacedIds(visitors.getInstance(NamespacedIdentifiableCollector.class).getIdentifiables(), primaryGroupName);

		ExecutionMetrics.reportQueryClassUsage(query.getClass(), primaryGroupName);

		final Namespace namespace = datasetRegistry.get(dataset.getId());
		final ExecutionManager executionManager = namespace.getExecutionManager();


		// If this is only a re-executing query, try to execute the underlying query instead.
		{
			final Optional<ManagedExecutionId> executionId = visitors.getInstance(QueryUtils.OnlyReusingChecker.class).getOnlyReused();

			final Optional<ManagedExecution<?>>
					execution =
					executionId.map(id -> tryReuse(query, id, datasetRegistry, config, executionManager, subject.getUser()));

			if (execution.isPresent()) {
				return execution.get();
			}
		}

		// Execute the query
		return executionManager.runQuery(datasetRegistry, query, subject.getUser(), dataset, config, system);
	}

	/**
	 * Determine if the submitted query does reuse ONLY another query and restart that instead of creating another one.
	 */
	private ManagedExecution<?> tryReuse(QueryDescription query, ManagedExecutionId executionId, DatasetRegistry datasetRegistry, ConqueryConfig config, ExecutionManager executionManager, User user) {

		ManagedExecution<?> execution = datasetRegistry.getMetaRegistry().resolve(executionId);

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

		// If the user is not the owner of the execution, we definitely create a new Execution, so the owner can cancel it
		if (!user.isOwner(execution)) {
			final ManagedExecution<?>
					newExecution =
					executionManager.createExecution(datasetRegistry, execution.getSubmitted(), user, execution.getDataset(), false);
			newExecution.setLabel(execution.getLabel());
			newExecution.setTags(execution.getTags().clone());
			storage.updateExecution(newExecution);
			execution = newExecution;
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


	public Stream<ExecutionStatus> getAllQueries(Dataset dataset, HttpServletRequest req, Subject subject, boolean allProviders) {
		Collection<ManagedExecution<?>> allQueries = storage.getAllExecutions();

		return getQueriesFiltered(dataset, RequestAwareUriBuilder.fromRequest(req), subject, allQueries, allProviders);
	}

	public Stream<ExecutionStatus> getQueriesFiltered(Dataset datasetId, UriBuilder uriBuilder, Subject subject, Collection<ManagedExecution<?>> allQueries, boolean allProviders) {
		Map<DatasetId, Set<Ability>> datasetAbilities = buildDatasetAbilityMap(subject, datasetRegistry);

		return allQueries.stream()
						 // The following only checks the dataset, under which the query was submitted, but a query can target more that
						 // one dataset.
						 .filter(q -> q.getDataset().equals(datasetId))
						 // to exclude subtypes from somewhere else
						 .filter(QueryProcessor::canFrontendRender)
						 .filter(Predicate.not(ManagedExecution::isSystem))
						 .filter(q -> q.getState().equals(ExecutionState.DONE) || q.getState().equals(ExecutionState.NEW))
						 .filter(q -> subject.isPermitted(q, Ability.READ))
						 .map(mq -> {
							 OverviewExecutionStatus status = mq.buildStatusOverview(
									 uriBuilder.clone(),
									 subject
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
									   .map(r -> r.generateResultURLs(exec, uriBuilder.clone(), allProviders))
									   .flatMap(Collection::stream).collect(Collectors.toList());

		status.setResultUrls(resultUrls);

		return status;
	}


	/**
	 * Test if the query is structured in a way the Frontend can render it.
	 */
	private static boolean canFrontendRender(ManagedExecution<?> q) {
		//TODO FK: should this be used to fill into canExpand instead of hiding the Executions?
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

	/**
	 * Cancel a running query: Sending cancellation to shards, which will cause them to stop executing them, results are not sent back, and incoming results will be discarded.
	 */
	public void cancel(Subject subject, Dataset dataset, ManagedExecution<?> query) {

		// Does not make sense to cancel a query that isn't running.
		if (!query.getState().equals(ExecutionState.RUNNING)) {
			return;
		}

		log.info("User[{}] cancelled Query[{}]", subject.getId(), query.getId());

		final Namespace namespace = getDatasetRegistry().get(dataset.getId());

		query.reset();

		namespace.sendToAll(new CancelQuery(query.getId()));
	}

	public void patchQuery(Subject subject, ManagedExecution<?> execution, MetaDataPatch patch) {

		log.info("Patching {} ({}) with patch: {}", execution.getClass().getSimpleName(), execution, patch);

		patch.applyTo(execution, storage, subject);
		storage.updateExecution(execution);

		// TODO remove this, since we don't translate anymore
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
			patch.applyTo(otherExecution, storage, subject);
			storage.updateExecution(execution);
		}
	}

	public void reexecute(Subject subject, ManagedExecution<?> query) {
		log.info("User[{}] reexecuted Query[{}]", subject.getId(), query);

		if (!query.getState().equals(ExecutionState.RUNNING)) {
			datasetRegistry.get(query.getDataset().getId())
						   .getExecutionManager()
						   .execute(getDatasetRegistry(), query, config);
		}
	}


	public void deleteQuery(Subject subject, ManagedExecution<?> execution) {
		log.info("User[{}] deleted Query[{}]", subject.getId(), execution.getId());

		datasetRegistry.get(execution.getDataset().getId())
					   .getExecutionManager() // Don't go over execution#getExecutionManager() as that's only set when query is initialized
					   .clearQueryResults(execution);

		storage.removeExecution(execution.getId());
	}

	public FullExecutionStatus getQueryFullStatus(ManagedExecution<?> query, Subject subject, UriBuilder url, Boolean allProviders) {

		query.initExecutable(datasetRegistry, config);

		Map<DatasetId, Set<Ability>> datasetAbilities = buildDatasetAbilityMap(subject, datasetRegistry);
		final FullExecutionStatus status = query.buildStatusFull(storage, subject, datasetRegistry, config);

		if (query.isReadyToDownload(datasetAbilities)) {
			setDownloadUrls(status, config.getResultProviders(), query, url, allProviders);
		}
		return status;
	}

	/**
	 * Try to resolve the external upload, if successful, create query for the subject and return id and statistics for that.
	 */
	public ExternalUploadResult uploadEntities(Subject subject, Dataset dataset, ExternalUpload upload) {

		final CQExternal.ResolveStatistic statistic =
				CQExternal.resolveEntities(upload.getValues(), upload.getFormat(),
										   datasetRegistry.get(dataset.getId()).getStorage().getIdMapping(),
										   config.getFrontend().getQueryUpload(),
										   config.getLocale().getDateReader()
				);

		// Resolving nothing is a problem thus we fail.
		if (statistic.getResolved().isEmpty()) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
												  .entity(new ExternalUploadResult(null, 0, statistic.getUnresolvedId(), statistic.getUnreadableDate()))
												  .build());
		}

		final ConceptQuery query = new ConceptQuery(new CQExternal(upload.getFormat(), upload.getValues()));

		// We only create the Query, really no need to execute it as it's only useful for composition.
		final ManagedQuery execution =
				((ManagedQuery) datasetRegistry.get(dataset.getId()).getExecutionManager()
											   .createExecution(datasetRegistry, query, subject.getUser(), dataset, false));

		execution.setLastResultCount((long) statistic.getResolved().size());

		if (upload.getLabel() != null) {
			execution.setLabel(upload.getLabel());
		}

		execution.initExecutable(datasetRegistry, config);

		return new ExternalUploadResult(
				execution.getId(),
				statistic.getResolved().size(),
				statistic.getUnresolvedId(),
				statistic.getUnreadableDate()
		);
	}

	/**
	 * Execute a {@link TableExportQuery} for a single Entity on some Connectors.
	 *
	 * @return
	 * @implNote we don't do anything special here, this request could also be made manually. We however want to encapsulate this behaviour to shield the frontend from knowing too much about the query engine.
	 */
	public List<URL> getSingleEntityExport(Subject subject, UriBuilder uriBuilder, String idKind, String entity, List<Connector> sources, Dataset dataset, Range<LocalDate> dateRange) {

		final ConceptQuery entitySelectQuery =
				new ConceptQuery(new CQDateRestriction(dateRange, new CQExternal(List.of(idKind), new String[][]{{"HEAD"}, {entity}})));

		final TableExportQuery exportQuery = new TableExportQuery(entitySelectQuery);
		exportQuery.setTables(
				sources.stream()
					   .map(source -> new CQUnfilteredTable(source, null))
					   .collect(Collectors.toList())
		);

		final ManagedExecution<?> execution = postQuery(dataset, exportQuery, subject, true);


		// collect id immediately so it does not get sucked into closure
		final ManagedExecutionId id = execution.getId();

		while (execution.awaitDone(10, TimeUnit.SECONDS) == ExecutionState.RUNNING) {
			log.trace("Still waiting for {}", id);
		}

		if (execution.getState() == ExecutionState.FAILED) {
			throw ConqueryError.ContextError.fromErrorInfo(execution.getError());
		}

		// Use the provided format name to find the respective provider.
		return config.getResultProviders().stream()
					 .map(resultRendererProvider -> resultRendererProvider.generateResultURLs(execution, uriBuilder.clone(), true))
					 .flatMap(Collection::stream)
					 .collect(Collectors.toList());

	}
}
