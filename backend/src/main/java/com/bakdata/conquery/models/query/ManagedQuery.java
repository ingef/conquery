package com.bakdata.conquery.models.query;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import c10n.C10N;
import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.internationalization.CQElementC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.query.concept.SecondaryIdQuery;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.ResultCSVResource;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString(callSuper = true)
@Slf4j
@CPSType(base = ManagedExecution.class, id = "MANAGED_QUERY")
public class ManagedQuery extends ManagedExecution<ShardResult> {

	// Needs to be resolved externally before being executed
	private IQuery query;

	@JsonIgnore
	protected transient Namespace namespace;
	/**
	 * The number of contained entities the last time this query was executed.
	 *
	 * @param lastResultCount the new count for JACKSON
	 * @returns the number of contained entities
	 */
	private Long lastResultCount;
	
	//we don't want to store or send query results or other result metadata
	@JsonIgnore
	private transient int involvedWorkers;
	@JsonIgnore
	private transient int executingThreads;
	@JsonIgnore
	private transient List<ColumnDescriptor> columnDescriptions;
	@JsonIgnore
	private transient List<EntityResult> results = new ArrayList<>();

	public ManagedQuery(IQuery query, UserId owner, DatasetId submittedDataset) {
		super(owner, submittedDataset);
		this.query = query;
	}

	@Override
	protected void doInitExecutable(@NonNull DatasetRegistry namespaces) {
		this.namespace = namespaces.get(getDataset());
		this.involvedWorkers = namespace.getWorkers().size();
		query.resolve(new QueryResolveContext(getDataset(), namespaces));
		if(label == null) {
			label = makeAutoLabel(namespaces);
		}
	}
	
	@Override
	public void addResult(@NonNull MetaStorage storage, ShardResult result) {
		log.debug("Received Result[size={}] for Query[{}]", result.getResults().size(), result.getQueryId());

		if(result.getError().isPresent()) {
			fail(storage, result.getError().get());
		}

		synchronized (getExecution()) {
			executingThreads--;
			results.addAll(result.getResults());
			if (executingThreads == 0 && state == ExecutionState.RUNNING) {
				finish(storage, ExecutionState.DONE);
			}
		}
	}

	@Override
	public void start() {
		super.start();
		synchronized (getExecution()) {
			executingThreads = involvedWorkers;
		}
		

		if(results != null)
			results.clear();
		else
			results = new ArrayList<>();
	}

	@Override
	protected void finish(@NonNull MetaStorage storage, ExecutionState executionState) {
		lastResultCount = query.countResults(results);

		super.finish(storage, executionState);
	}

	public Stream<ContainedEntityResult> fetchContainedEntityResult() {
		return results.stream().flatMap(ContainedEntityResult::filterCast);
	}

	@JsonIgnore
	public ResultInfoCollector collectResultInfos() {
		return query.collectResultInfos();
	}
	
	@Override
	protected void setStatusBase(@NonNull MetaStorage storage, @NonNull User user, @NonNull ExecutionStatus status, UriBuilder url) {
		super.setStatusBase(storage, user, status, url);
		status.setNumberOfResults(lastResultCount);

		status.setQueryType(query.getClass().getAnnotation(CPSType.class).id());

		if (query instanceof SecondaryIdQuery) {
			status.setSecondaryId(((SecondaryIdQuery) query).getSecondaryId().getId());
		}
	}
	
	@Override
	protected void setAdditionalFieldsForStatusWithColumnDescription(@NonNull MetaStorage storage, UriBuilder url, User user, ExecutionStatus.Full status, DatasetRegistry datasetRegistry) {
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
		List<ColumnDescriptor> columnDescriptions = new ArrayList<>();
		// First add the id columns to the descriptor list. The are the first columns
		for (String header : ConqueryConfig.getInstance().getIdMapping().getPrintIdFields()) {
			columnDescriptions.add(ColumnDescriptor.builder()
				.label(header)
				.type(ConqueryConstants.ID_TYPE)
				.build());
		}
		// Then all columns that originate from selects and static aggregators
		PrintSettings settings = new PrintSettings(true, I18n.LOCALE.get(), datasetRegistry);
		collectResultInfos().getInfos().forEach(info -> columnDescriptions.add(info.asColumnDescriptor(settings)));
		return columnDescriptions;
	}

	@Override
	public Set<NamespacedId> getUsedNamespacedIds() {
		NamespacedIdCollector collector = new NamespacedIdCollector();
		query.visit(collector);
		return collector.getIds();
	}

	@Override
	public Map<ManagedExecutionId,QueryPlan> createQueryPlans(QueryPlanContext context) {
		if(context.getDataset().equals(getDataset())) {			
			return Map.of(this.getId(), query.createQueryPlan(context));
		}
		log.trace("Did not create a QueryPlan for the query {} because the plan corresponds to dataset {} but the execution worker belongs to {}.", getId(), getDataset(), context.getDataset());
		return Collections.emptyMap();
	}

	@Override
	public ShardResult getInitializedShardResult(Entry<ManagedExecutionId, QueryPlan> entry) {
		ShardResult result = new ShardResult();
		result.setQueryId(getId());
		return result;
	}

	@Override
	public Set<Namespace> getRequiredDatasets() {
		return Set.of(namespace);
	}

	@Override
	public QueryDescription getSubmitted() {
		return query;
	}

	@Override
	public StreamingOutput getResult(Function<ContainedEntityResult,ExternalEntityId> idMapper, PrintSettings settings, Charset charset, String lineSeparator) {
		return ResultCSVResource.resultAsStreamingOutput(this.getId(), settings, List.of(this), idMapper, charset, lineSeparator);
	}
	
	@Override
	protected URL getDownloadURLInternal(@NonNull UriBuilder url) throws MalformedURLException, IllegalArgumentException, UriBuilderException {
		return url
			.path(ResultCSVResource.class)
			.resolveTemplate(ResourceConstants.DATASET, dataset.getName())
			.path(ResultCSVResource.class, ResultCSVResource.GET_CSV_PATH_METHOD)
			.resolveTemplate(ResourceConstants.QUERY, getId().toString())
			.build()
			.toURL();
	}

	private static final int MAX_CONCEPT_LABEL_CONCAT_LENGTH = 70;
	
	/**
	 * Creates a default label based on the submitted {@link QueryDescription}.
	 * The Label is customized by mentioning that a description contained a 
	 * {@link CQExternal}, {@link CQReusedQuery} or {@link CQConcept}, in this order.
	 * In case of one ore more {@link CQConcept} the distinct labels of the concepts are chosen
	 * and concatinated until a length of {@value #MAX_CONCEPT_LABEL_CONCAT_LENGTH} is reached.
	 * All further labels are dropped.
	 */
	@Override
	protected void makeDefaultLabel(final StringBuilder sb, DatasetRegistry datasetRegistry) {
		final Map<Class<? extends Visitable>,List<Visitable>> sortedContents = new HashMap<>();
		
		int sbStartSize = sb.length();
		
		QueryVisitor visitor = new QueryVisitor() {
			
			@Override
			public void accept(Visitable t) {
				sortedContents.computeIfAbsent(t.getClass(), (clazz) -> new ArrayList<>()).add(t);
			}
		};
		query.visit(visitor);
		
		// Check for CQExternal
		List<Visitable> externals = sortedContents.computeIfAbsent(CQExternal.class, (clazz)-> List.of());
		if(!externals.isEmpty()) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).external());
		}
		
		// Check for CQReused
		if( !sortedContents.computeIfAbsent(CQReusedQuery.class, (clazz)-> List.of()).isEmpty()) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).reused());
		}
		
		// Check for CQConcept
		final AtomicInteger length = new AtomicInteger();
		String usedConcepts = sortedContents.computeIfAbsent(CQConcept.class, (clazz)-> List.of()).stream()
			.map((CQConcept.class::cast))
			.map(c -> makeLabelWithRootAndChild(datasetRegistry, c))
			.distinct()
			.filter((s) -> !Strings.isNullOrEmpty(s))
			.takeWhile(elem -> length.addAndGet(elem.length()) < MAX_CONCEPT_LABEL_CONCAT_LENGTH)
			.collect(Collectors.joining(" "));
		
		if (sb.length() > 0 && usedConcepts.length() > 0) {
			sb.append(" ");
		}
		sb.append(usedConcepts);

		// If not all Concept could be included in the name, point that out
		if (length.get() > MAX_CONCEPT_LABEL_CONCAT_LENGTH) {
			sb.append(" ").append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).furtherConcepts());
		}
		
		// Fallback to id if nothing could be extracted from the query description
		if(sbStartSize == sb.length()) {
			sb.append(getId().getExecution());
		}
	}

	private static String makeLabelWithRootAndChild(DatasetRegistry datasetRegistry, CQConcept cqConcept){
		String cqConceptLabel = cqConcept.getLabel();
		if (cqConceptLabel == null){
			return "";
		}

		if(cqConcept.getIds().isEmpty()){
			return cqConceptLabel.replace(" ", "-"); // This is usually an illegal case, an CQConcept must have at least one id, but this code should never fail
		}

		ConceptElementId<?> id = cqConcept.getIds().iterator().next();

		Concept<?> concept = datasetRegistry.resolve(id.findConcept());
		String conceptLabel = concept.getLabel();
		if(cqConceptLabel.equalsIgnoreCase(conceptLabel)){
			return cqConceptLabel.replace(" ", "-");
		}

		// Concat everything with dashes
		return (conceptLabel + "-" + cqConceptLabel).replace(" ", "-");
	}
}
