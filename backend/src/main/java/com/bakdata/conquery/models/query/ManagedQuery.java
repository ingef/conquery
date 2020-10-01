package com.bakdata.conquery.models.query;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.ResultCSVResource;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
	public void initExecutable(@NonNull DatasetRegistry namespaces) {
		synchronized (getExecution()) {
			this.namespace = namespaces.get(getDataset());
			this.involvedWorkers = namespace.getWorkers().size();
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
		lastResultCount = results.stream().flatMap(ContainedEntityResult::filterCast).count();

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
	protected void setStatusBase(@NonNull MetaStorage storage, URLBuilder url, @NonNull User user, @NonNull ExecutionStatus status) {
		super.setStatusBase(storage, url, user, status);
		status.setNumberOfResults(lastResultCount);
	}
	
	@Override
	protected void setAdditionalFieldsForStatusWithColumnDescription(@NonNull MetaStorage storage, URLBuilder url, User user, ExecutionStatus status) {
		super.setAdditionalFieldsForStatusWithColumnDescription(storage, url, user, status);
		if (columnDescriptions == null) {
			columnDescriptions = generateColumnDescriptions();
		}
		status.setColumnDescriptions(columnDescriptions);
	}

	/**
	 * Generates a description of each column that will appear in the resulting csv.
	 */
	public List<ColumnDescriptor> generateColumnDescriptions() {
		List<ColumnDescriptor> columnDescriptions = new ArrayList<>();
		// First add the id columns to the descriptor list. The are the first columns
		for (String header : ConqueryConfig.getInstance().getIdMapping().getPrintIdFields()) {
			columnDescriptions.add(ColumnDescriptor.builder()
				.label(header)
				.type(ConqueryConstants.ID_TYPE)
				.build());
		}
		// Then all columns that originate from selects and static aggregators
		PrintSettings settings = new PrintSettings(true, I18n.LOCALE.get());
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
	public StreamingOutput getResult(IdMappingState mappingState, PrintSettings settings, Charset charset, String lineSeparator) {
		return ResultCSVResource.resultAsStreamingOutput(this.getId(), settings, List.of(this), mappingState, charset, lineSeparator);
	}
	
	@Override
	protected URL getDownloadURL(URLBuilder url) {
		return url.set(ResourceConstants.DATASET, dataset.getName()).set(ResourceConstants.QUERY, getId().toString())
			.to(ResultCSVResource.GET_CSV_PATH).get();
	}
}
