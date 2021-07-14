package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.UriBuilder;

import c10n.C10N;
import com.bakdata.conquery.apiv1.ExecutionStatus;
import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQExternal;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.internationalization.CQElementC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString(callSuper = true)
@Slf4j
@CPSType(base = ManagedExecution.class, id = "MANAGED_QUERY")
@NoArgsConstructor
public class ManagedQuery extends ManagedExecution<ShardResult> implements SingleTableResult {

	private static final int MAX_CONCEPT_LABEL_CONCAT_LENGTH = 70;
	@JsonIgnore
	protected transient Namespace namespace;
	// Needs to be resolved externally before being executed
	private Query query;
	/**
	 * The number of contained entities the last time this query was executed.
	 */
	private Long lastResultCount;

	//we don't want to store or send query results or other result metadata
	@JsonIgnore
	private transient int involvedWorkers;
	@JsonIgnore
	private transient AtomicInteger executingThreads = new AtomicInteger(0);
	@JsonIgnore
	private transient ConqueryConfig config;
	@JsonIgnore
	private transient List<ColumnDescriptor> columnDescriptions;


	public ManagedQuery(Query query, User owner, Dataset submittedDataset) {
		super(owner, submittedDataset);
		this.query = query;
	}

	@Override
	protected void doInitExecutable(@NonNull DatasetRegistry namespaces, ConqueryConfig config) {
		this.config = config;

		namespace = namespaces.get(getDataset().getId());

		involvedWorkers = namespace.getWorkers().size();

		query.resolve(new QueryResolveContext(getDataset(), namespaces, config, null));
	}

	@Override
	public void addResult(@NonNull MetaStorage storage, ShardResult result) {
		log.debug("Received Result[size={}] for Query[{}]", result.getResults().size(), result.getQueryId());

		if (result.getError().isPresent()) {
			fail(storage, result.getError().get());
			return;
		}

		final int remaining = executingThreads.decrementAndGet();

		getExecutionManager().addQueryResult(this, result.getResults());

		if (remaining == 0 && getState() == ExecutionState.RUNNING) {
			finish(storage, ExecutionState.DONE);
		}
	}

	public Stream<EntityResult> streamResults() {
		return getExecutionManager().streamQueryResults(this);
	}

	@Override
	protected void finish(@NonNull MetaStorage storage, ExecutionState executionState) {
		lastResultCount = query.countResults(streamResults());

		super.finish(storage, executionState);
	}

	@Override
	public void start() {
		super.start();

		executingThreads.set(involvedWorkers);
	}

	@Override
	protected void setStatusBase(@NonNull User user, @NonNull ExecutionStatus status, UriBuilder url, Map<DatasetId, Set<Ability>> datasetAbilities) {
		super.setStatusBase(user, status, url, datasetAbilities);
		status.setNumberOfResults(lastResultCount);

		status.setQueryType(query.getClass().getAnnotation(CPSType.class).id());

		if (query instanceof SecondaryIdQuery) {
			status.setSecondaryId(((SecondaryIdQuery) query).getSecondaryId().getId());
		}
	}

	@Override
	protected void setAdditionalFieldsForStatusWithColumnDescription(@NonNull MetaStorage storage, UriBuilder url, User user, FullExecutionStatus status, DatasetRegistry datasetRegistry) {
		super.setAdditionalFieldsForStatusWithColumnDescription(storage, url, user, status, datasetRegistry);
		if (columnDescriptions == null) {
			columnDescriptions = generateColumnDescriptions(datasetRegistry);
		}
		status.setColumnDescriptions(columnDescriptions);
	}

	/**
	 * Generates a description of each column that will appear in the resulting csv.
	 */
	public List<ColumnDescriptor> generateColumnDescriptions(DatasetRegistry datasetRegistry) {
		Preconditions.checkArgument(isInitialized(), "The execution must have been initialized first");
		List<ColumnDescriptor> columnDescriptions = new ArrayList<>();
		// First add the id columns to the descriptor list. The are the first columns
		for (String header : config.getIdMapping().getPrintIdFields()) {
			columnDescriptions.add(ColumnDescriptor.builder()
												   .label(header)
												   .type(ResultType.IdT.INSTANCE.typeInfo())
												   .build());
		}
		// Then all columns that originate from selects and static aggregators
		PrintSettings settings = new PrintSettings(true, I18n.LOCALE.get(), datasetRegistry, config, null);

		getResultInfo().forEach(info -> columnDescriptions.add(info.asColumnDescriptor(settings)));
		return columnDescriptions;
	}

	@JsonIgnore
	public List<ResultInfo> getResultInfo() {
		return query.collectResultInfos().getInfos();
	}

	@Override
	@JsonIgnore
	public Set<NamespacedIdentifiable<?>> getUsedNamespacedIds() {
		NamespacedIdentifiableCollector collector = new NamespacedIdentifiableCollector();
		query.visit(collector);
		return collector.getIdentifiables();
	}

	@Override
	public Set<Namespace> getRequiredDatasets() {
		return Set.of(namespace);
	}

	@Override
	@JsonIgnore
	public QueryDescription getSubmitted() {
		return query;
	}

	/**
	 * Creates a default label based on the submitted {@link QueryDescription}.
	 * The Label is customized by mentioning that a description contained a
	 * {@link CQExternal}, {@link CQReusedQuery} or {@link CQConcept}, in this order.
	 * In case of one ore more {@link CQConcept} the distinct labels of the concepts are chosen
	 * and concatinated until a length of {@value #MAX_CONCEPT_LABEL_CONCAT_LENGTH} is reached.
	 * All further labels are dropped.
	 */
	@Override
	protected String makeDefaultLabel(PrintSettings cfg) {
		final StringBuilder sb = new StringBuilder();

		final Map<Class<? extends Visitable>, List<Visitable>> sortedContents =
				Visitable.stream(query)
						 .collect(Collectors.groupingBy(Visitable::getClass));

		int sbStartSize = sb.length();

		// Check for CQExternal
		List<Visitable> externals = sortedContents.getOrDefault(CQExternal.class, Collections.emptyList());
		if (!externals.isEmpty()) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).external());
		}

		// Check for CQReused
		if (sortedContents.containsKey(CQReusedQuery.class)) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).reused());
		}


		// Check for CQConcept
		if (sortedContents.containsKey(CQConcept.class)) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			// Track length of text we are appending for concepts.
			final AtomicInteger length = new AtomicInteger();

			sortedContents.get(CQConcept.class)
						  .stream()
						  .map(CQConcept.class::cast)

						  .map(c -> makeLabelWithRootAndChild(c, cfg))
						  .filter(Predicate.not(Strings::isNullOrEmpty))
						  .distinct()

						  .takeWhile(elem -> length.addAndGet(elem.length()) < MAX_CONCEPT_LABEL_CONCAT_LENGTH)
						  .forEach(label -> sb.append(label).append(" "));

			// Last entry will output one Space that we don't want
			sb.deleteCharAt(sb.length() - 1);

			// If not all Concept could be included in the name, point that out
			if (length.get() > MAX_CONCEPT_LABEL_CONCAT_LENGTH) {
				sb.append(" ").append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).furtherConcepts());
			}
		}



		// Fallback to id if nothing could be extracted from the query description
		if (sbStartSize == sb.length()) {
			sb.append(getId().getExecution());
		}

		return sb.toString();
	}

	@Override
	public WorkerMessage createExecutionMessage() {
		return new ExecuteQuery(getId(), getQuery());
	}

	private static String makeLabelWithRootAndChild(CQConcept cqConcept, PrintSettings cfg) {
		String label = cqConcept.getUserOrDefaultLabel(cfg.getLocale());
		if (label == null) {
			label = cqConcept.getConcept().getLabel();
		}

		// Concat everything with dashes
		return label.replace(" ", "-");
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}
}
