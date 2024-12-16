package com.bakdata.conquery.apiv1;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.execution.OverviewExecutionStatus;
import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.ExternalUpload;
import com.bakdata.conquery.apiv1.query.ExternalUploadResult;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.apiv1.query.concept.specific.external.EntityResolver;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.preview.EntityPreviewExecution;
import com.bakdata.conquery.models.query.preview.EntityPreviewForm;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.resultinfo.printers.JavaResultPrinters;
import com.bakdata.conquery.models.query.statistics.ResultStatistics;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.types.SemanticType;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.bakdata.conquery.util.io.IdColumnUtil;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class QueryProcessor {

	@Inject
	private DatasetRegistry<? extends Namespace> datasetRegistry;
	@Inject
	private MetaStorage storage;
	@Inject
	private ConqueryConfig config;
	@Inject
	private Validator validator;


	public List<? extends ExecutionStatus> getAllQueries(Dataset dataset, HttpServletRequest req, Subject subject, boolean allProviders) {
		try(Stream<ManagedExecution> allQueries = storage.getAllExecutions()) {
			return getQueriesFiltered(dataset.getId(), RequestAwareUriBuilder.fromRequest(req), subject, allQueries, allProviders).toList();
		}
	}

	public Stream<? extends ExecutionStatus> getQueriesFiltered(DatasetId datasetId, UriBuilder uriBuilder, Subject subject, Stream<ManagedExecution> allQueries, boolean allProviders) {

		return allQueries
				// The following only checks the dataset, under which the query was submitted, but a query can target more that
				// one dataset.
				.filter(q -> q.getDataset().equals(datasetId))
				// to exclude subtypes from somewhere else
				.filter(QueryProcessor::canFrontendRender)
				.filter(Predicate.not(ManagedExecution::isSystem))
				.filter(q -> {
							ExecutionState state = q.getState();
							return state == ExecutionState.NEW || state == ExecutionState.DONE;
						})
				.filter(q -> subject.isPermitted(q, Ability.READ))
				.map(mq -> {
					try {
						final OverviewExecutionStatus status = mq.buildStatusOverview(subject);

						if (mq.isReadyToDownload()) {
							status.setResultUrls(getResultAssets(config.getResultProviders(), mq, uriBuilder, allProviders));
						}
						return status;
					}
					catch (Exception e) {
						log.error("FAILED building status for {}", mq, e);
					}
					return null;
				})
				.filter(Objects::nonNull);
	}

	/**
	 * Test if the query is structured in a way the Frontend can render it.
	 */
	private static boolean canFrontendRender(ManagedExecution q) {
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
	 * Sets the result urls for the given result renderer. Result urls are only rendered for providers that match the
	 * result type of the execution.
	 *
	 * @param renderer     The renderer that are requested for a result url generation.
	 * @param exec         The execution that is used for generating the url
	 * @param uriBuilder   The Uribuilder with the base configuration to generate the urls
	 * @param allProviders If true, forces {@link ResultRendererProvider} to return an URL if possible.
	 * @return The modified status
	 */
	public static List<ResultAsset> getResultAssets(List<ResultRendererProvider> renderer, ManagedExecution exec, UriBuilder uriBuilder, boolean allProviders) {

		return renderer.stream()
					   .map(rendererProvider -> {
						   try {
							   return rendererProvider.generateResultURLs(exec, uriBuilder.clone(), allProviders);
						   }
						   catch (Exception e) {
							   log.error("Cannot generate result urls for execution '{}' with provider {}", exec.getId(), rendererProvider, e);
							   return null;
						   }
					   })
					   .filter(Objects::nonNull)
					   .flatMap(Collection::stream)
					   .toList();

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
	public void cancel(Subject subject, ManagedExecution query) {

		// Does not make sense to cancel a query that isn't running.
		ExecutionManager executionManager = datasetRegistry.get(query.getDataset()).getExecutionManager();
		if (!query.getState().equals(ExecutionState.RUNNING)) {
			return;
		}

		log.info("User[{}] cancelled Query[{}]", subject.getId(), query.getId());

		executionManager.cancelExecution(query);
	}

	public void patchQuery(Subject subject, ManagedExecution execution, MetaDataPatch patch) {

		log.info("Patching {} ({}) with patch: {}", execution.getClass().getSimpleName(), execution, patch);

		// If the patch shares the execution, we also share all subQueries
		if (patch.getGroups() != null && !patch.getGroups().isEmpty()) {


			for (ManagedExecutionId subExecutionId : execution.getSubmitted().collectRequiredQueries()) {

				if (!subject.isPermitted(subExecutionId, Ability.READ)) {
					log.warn("Not sharing {} as User {} is not allowed to see it themselves.", subExecutionId, subject);
					continue;
				}

				final ConqueryPermission canReadQuery = subExecutionId.createPermission(Set.of(Ability.READ));

				final Set<GroupId> groupsToShareWith = new HashSet<>(patch.getGroups());

				// Find all groups the query is already shared with, so we do not remove them, as patch is absolute
				for (Group group : storage.getAllGroups().toList()) {
					if (groupsToShareWith.contains(group.getId())) {
						continue;
					}

					final Set<ConqueryPermission> effectivePermissions = group.getEffectivePermissions();

					if (effectivePermissions.stream().anyMatch(perm -> perm.implies(canReadQuery))) {
						groupsToShareWith.add(group.getId());
					}
				}

				final MetaDataPatch sharePatch = MetaDataPatch.builder()
															  .groups(new ArrayList<>(groupsToShareWith))
															  .build();

				patchQuery(subject, subExecutionId.resolve(), sharePatch);
			}
		}

		patch.applyTo(execution, storage, subject);
		storage.updateExecution(execution);
	}

	public void reexecute(Subject subject, ManagedExecution query) {
		log.info("User[{}] reexecuted Query[{}]", subject.getId(), query);

		if (!query.getState().equals(ExecutionState.RUNNING)) {
			final Namespace namespace = query.getNamespace();

			namespace.getExecutionManager().execute(query);
		}
	}

	public void deleteQuery(Subject subject, ManagedExecutionId execution) {
		log.info("User[{}] deleted Query[{}]", subject.getId(), execution);

		datasetRegistry.get(execution.getDataset())
					   .getExecutionManager() // Don't go over execution#getExecutionManager() as that's only set when query is initialized
					   .clearQueryResults(execution);

		storage.removeExecution(execution);
	}

	public ExecutionState awaitDone(ManagedExecution query, int time, TimeUnit unit) {
		final Namespace namespace = datasetRegistry.get(query.getDataset());
		return namespace.getExecutionManager().awaitDone(query, time, unit);
	}

	public FullExecutionStatus getQueryFullStatus(ManagedExecution query, Subject subject, UriBuilder url, Boolean allProviders) {
		final Namespace namespace = datasetRegistry.get(query.getDataset());

		query.initExecutable();

		final FullExecutionStatus status = query.buildStatusFull(subject, namespace);

		if (query.isReadyToDownload() && subject.isPermitted(namespace.getDataset(), Ability.DOWNLOAD)) {
			status.setResultUrls(getResultAssets(config.getResultProviders(), query, url, allProviders));
		}
		return status;
	}

	/**
	 * Try to resolve the external upload, if successful, create query for the subject and return id and statistics for that.
	 */
	public ExternalUploadResult uploadEntities(Subject subject, Dataset dataset, ExternalUpload upload) {

		final Namespace namespace = datasetRegistry.get(dataset.getId());
		final EntityResolver.ResolveStatistic statistic = namespace.getEntityResolver().resolveEntities(
				upload.getValues(),
				upload.getFormat(),
				namespace.getStorage().getIdMapping(),
				config.getIdColumns(),
				config.getLocale().getDateReader(),
				upload.isOneRowPerEntity()
		);

		// Resolving nothing is a problem thus we fail.
		if (statistic.getResolved().isEmpty()) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
												  .entity(new ExternalUploadResult(null, 0, statistic.getUnresolvedId(), statistic.getUnreadableDate()))
												  .build());
		}

		final ConceptQuery query = new ConceptQuery(new CQExternal(upload.getFormat(), upload.getValues(), upload.isOneRowPerEntity()));

		// We only create the Query, really no need to execute it as it's only useful for composition.
		final ManagedQuery
				execution =
				((ManagedQuery) namespace
						.getExecutionManager()
						.createExecution(query, subject.getId(), namespace, false));

		execution.setLastResultCount((long) statistic.getResolved().size());

		if (upload.getLabel() != null) {
			execution.setLabel(upload.getLabel());
		}

		execution.initExecutable();

		return new ExternalUploadResult(execution.getId(), statistic.getResolved().size(), statistic.getUnresolvedId(), statistic.getUnreadableDate());
	}

	/**
	 * Create and submit {@link EntityPreviewForm} for subject on to extract sources for entity, and extract some additional infos to be used as infocard.
	 */
	public FullExecutionStatus getSingleEntityExport(Subject subject, UriBuilder uriBuilder, String idKind, String entity, List<ConnectorId> sources, Dataset dataset, Range<LocalDate> dateRange) {

		subject.authorize(dataset, Ability.ENTITY_PREVIEW);
		subject.authorize(dataset, Ability.PRESERVE_ID);

		Namespace namespace = datasetRegistry.get(dataset.getId());
		final PreviewConfig previewConfig = namespace.getPreviewConfig();
		final EntityPreviewForm form =
				EntityPreviewForm.create(entity, idKind, dateRange, sources, previewConfig.getSelects(), previewConfig.getTimeStratifiedSelects(), datasetRegistry);

		// Validate our own form because we provide it directly to the processor, which does not validate.
		ValidatorHelper.failOnError(log, validator.validate(form));

		// TODO make sure that subqueries are also system
		// TODO do not persist system queries
		final EntityPreviewExecution execution = (EntityPreviewExecution) postQuery(dataset, form, subject, true);


		ExecutionManager executionManager = namespace.getExecutionManager();
		if (executionManager.awaitDone(execution, 10, TimeUnit.SECONDS) == ExecutionState.RUNNING) {
			log.warn("Still waiting for {} after 10 Seconds.", execution.getId());
			throw new ConqueryError.ExecutionProcessingTimeoutError();
		}


		if (execution.getState() == ExecutionState.FAILED) {
			throw new ConqueryError.ExecutionProcessingError();
		}

		// Workaround update our execution as the lastresultcount was set in the background
		final EntityPreviewExecution executionFinished = (EntityPreviewExecution) execution.getId().resolve();
		executionFinished.initExecutable();

		final FullExecutionStatus status = execution.buildStatusFull(subject, namespace);
		status.setResultUrls(getResultAssets(config.getResultProviders(), executionFinished, uriBuilder, false));
		return status;
	}

	/**
	 * Creates a query for all datasets, then submits it for execution on the
	 * intended dataset.
	 */
	public ManagedExecution postQuery(Dataset dataset, QueryDescription query, Subject subject, boolean system) {

		log.info("Query posted on Dataset[{}] by User[{{}].", dataset.getId(), subject.getId());

		// This maps works as long as we have query visitors that are not configured in anyway.
		// So adding a visitor twice would replace the previous one but both would have yielded the same result.
		// For the future a better data structure might be desired that also regards similar QueryVisitors of different configuration
		final List<QueryVisitor> visitors = new ArrayList<>();
		query.addVisitors(visitors);

		// Initialize checks that need to traverse the query tree
		QueryUtils.OnlyReusingChecker onlyReusingChecker = new QueryUtils.OnlyReusingChecker();
		visitors.add(onlyReusingChecker);
		NamespacedIdentifiableCollector namespacedIdentifiableCollector = new NamespacedIdentifiableCollector();
		visitors.add(namespacedIdentifiableCollector);

		final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(subject, storage).map(Group::getName).orElse("none");
		ExecutionMetrics.QueryMetricsReporter queryMetricsReporter = new ExecutionMetrics.QueryMetricsReporter(primaryGroupName);
		visitors.add(queryMetricsReporter);


		// Chain all Consumers
		Consumer<Visitable> consumerChain = QueryUtils.getNoOpEntryPoint();
		for (QueryVisitor visitor : visitors) {
			consumerChain = consumerChain.andThen(visitor);
		}

		// Apply consumers to the query tree
		query.visit(consumerChain);


		query.authorize(subject, dataset, visitors, storage);
		// After all authorization checks we can now use the actual subject to invoke the query and do not to bubble down the Userish in methods


		ExecutionMetrics.reportNamespacedIds(namespacedIdentifiableCollector.getIdentifiables(), primaryGroupName);

		ExecutionMetrics.reportQueryClassUsage(query.getClass(), primaryGroupName);

		final Namespace namespace = datasetRegistry.get(dataset.getId());
		final ExecutionManager executionManager = namespace.getExecutionManager();


		// If this is only a re-executing query, try to execute the underlying query instead.
		{
			final Optional<ManagedExecutionId> executionId = onlyReusingChecker.getOnlyReused();

			final Optional<ManagedExecution>
					execution =
					executionId.map(id -> tryReuse(query, id, namespace, executionManager, subject.getUser()));

			if (execution.isPresent()) {
				return execution.get();
			}
		}

		// Execute the query
		return executionManager.runQuery(namespace, query, subject.getId(), system);
	}

	/**
	 * Determine if the submitted query does reuse ONLY another query and restart that instead of creating another one.
	 */
	private ManagedExecution tryReuse(QueryDescription query, ManagedExecutionId executionId, Namespace namespace, ExecutionManager executionManager, User user) {

		ManagedExecution execution = storage.getExecution(executionId);

		if (execution == null) {
			return null;
		}


		// Direct reuse only works if the queries are of the same type (As reuse reconstructs the Query for different types)
		if (!query.getClass().equals(execution.getSubmitted().getClass())) {
			return null;
		}

		// If SecondaryIds differ from selected and prior, we cannot reuse them.
		if (query instanceof SecondaryIdQuery) {
			final SecondaryIdDescriptionId selectedSecondaryId = ((SecondaryIdQuery) query).getSecondaryId();
			final SecondaryIdDescriptionId reusedSecondaryId = ((SecondaryIdQuery) execution.getSubmitted()).getSecondaryId();

			if (!selectedSecondaryId.equals(reusedSecondaryId)) {
				return null;
			}
		}

		// If the user is not the owner of the execution, we definitely create a new Execution, so the owner can cancel it
		if (!user.isOwner(execution)) {
			final ManagedExecution
					newExecution =
					executionManager.createExecution(execution.getSubmitted(), user.getId(), namespace, false);
			newExecution.setLabel(execution.getLabel());
			newExecution.setTags(execution.getTags().clone());
			storage.updateExecution(newExecution);
			execution = newExecution;
		}

		final ExecutionState state = execution.getState();
		if (state.equals(ExecutionState.RUNNING)) {
			log.trace("The Execution[{}] was already started and its state is: {}", execution.getId(), state);
			return execution;
		}

		log.trace("Re-executing Query {}", execution);

		executionManager.execute(execution);

		return execution;

	}

	/**
	 * Execute a basic query on a single concept and return only the included entities Id's.
	 */
	public Stream<Map<String, String>> resolveEntities(Subject subject, List<FilterValue<?>> filters, Dataset dataset) {
		if (filters.stream().map(fv -> fv.getFilter().getConnector()).distinct().count() != 1) {
			throw new BadRequestException("Query exactly one connector at once.");
		}

		final Namespace namespace = datasetRegistry.get(dataset.getId());

		final List<CQElement> queries = new ArrayList<>(filters.size());

		for (FilterValue<?> filter : filters) {
			final CQConcept cqConcept = new CQConcept();
			cqConcept.setElements(List.of(filter.getFilter().getConnector().getConcept()));

			final CQTable cqTable = new CQTable();

			cqTable.setFilters(List.of(filter));
			cqTable.setConnector(filter.getFilter().getConnector());
			cqTable.setConcept(cqConcept);

			cqConcept.setTables(List.of(cqTable));

			queries.add(cqConcept);
		}


		final QueryDescription query = new ConceptQuery(new CQOr(queries, Optional.of(false), DateAggregationAction.BLOCK));

		final ManagedExecution execution = postQuery(dataset, query, subject, true);

		if (namespace.getExecutionManager().awaitDone(execution, 10, TimeUnit.SECONDS) == ExecutionState.RUNNING) {
			log.warn("Still waiting for {} after 10 Seconds.", execution.getId());
			throw new ConqueryError.ExecutionProcessingTimeoutError();
		}

		if (execution.getState() == ExecutionState.FAILED) {
			throw new ConqueryError.ExecutionProcessingError();
		}

		final SingleTableResult result = (SingleTableResult) execution;


		final List<ColumnConfig> ids = config.getIdColumns()
											 .getIds().stream()
											 // We're only interested in returning printable ids
											 .filter(ColumnConfig::isPrint)
											 .collect(Collectors.toList());


		final Map<String, Integer> id2index = IntStream.range(0, ids.size())
													   .boxed()
													   .collect(Collectors.toMap(
															   idx -> ids.get(idx).getName(),
															   idx -> idx
													   ));

		final IdPrinter printer = IdColumnUtil.getIdPrinter(subject, execution, namespace, ids);

		// For each included entity emit a Map of { Id-Name -> Id-Value }
		return result.streamResults(OptionalLong.empty())
					 .map(printer::createId)
					 .map(entityPrintId -> {
						 final Map<String, String> out = new HashMap<>();

						 for (Map.Entry<String, Integer> entry : id2index.entrySet()) {
							 // Not all ExternalIds are expected to be set.
							 if (entityPrintId.getExternalId()[entry.getValue()] == null) {
								 continue;
							 }

							 out.put(entry.getKey(), entityPrintId.getExternalId()[entry.getValue()]);
						 }

						 return out;
					 })
					 .filter(Predicate.not(Map::isEmpty));
	}

	public <E extends ManagedExecution & SingleTableResult> ResultStatistics getResultStatistics(E managedQuery) {

		final Locale locale = I18n.LOCALE.get();
		final NumberFormat decimalFormat = NumberFormat.getNumberInstance(locale);
		decimalFormat.setMaximumFractionDigits(2);

		final NumberFormat integerFormat = NumberFormat.getNumberInstance(locale);


		final PrintSettings printSettings =
				new PrintSettings(true, locale, managedQuery.getNamespace(), config, null, null, decimalFormat, integerFormat);
		final UniqueNamer uniqueNamer = new UniqueNamer(printSettings);

		managedQuery.initExecutable();

		final List<ResultInfo> resultInfos = managedQuery.getResultInfos();

		final Optional<ResultInfo>
				dateInfo =
				resultInfos.stream().filter(info -> info.getSemantics().contains(new SemanticType.EventDateT())).findFirst();

		final Optional<Integer> dateIndex = dateInfo.map(resultInfos::indexOf);

		return ResultStatistics.collectResultStatistics(managedQuery,
														resultInfos,
														dateInfo,
														dateIndex,
														printSettings,
														uniqueNamer,
														config,
														new JavaResultPrinters()
		);
	}

}
