package com.bakdata.conquery.models.execution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.SubmittedQuery;
import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.ResultCSVResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Setter
@ToString
@Slf4j
@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public abstract class ManagedExecution<R extends ShardResult> extends IdentifiableImpl<ManagedExecutionId> {

	protected DatasetId dataset;
	protected UUID queryId = UUID.randomUUID();
	@NotEmpty
	protected String label = queryId.toString();
	protected LocalDateTime creationTime = LocalDateTime.now();
	@Nullable
	protected UserId owner;

	@NotNull
	private String[] tags = ArrayUtils.EMPTY_STRING_ARRAY;

	protected boolean machineGenerated;

	// we don't want to store or send query results or other result metadata
	@JsonIgnore
	protected transient ExecutionState state = ExecutionState.NEW;
	@JsonIgnore
	private final transient CountDownLatch execution = new CountDownLatch(1);
	@JsonIgnore
	private transient LocalDateTime startTime;
	@JsonIgnore
	protected transient LocalDateTime finishTime;
	@JsonIgnore
	protected final transient MasterMetaStorage storage;

	public ManagedExecution(MasterMetaStorage storage, UserId owner, DatasetId submittedDataset) {
		this.storage = storage;
		this.owner = owner;
		this.dataset = submittedDataset;
	}

	/**
	 * Executed right before execution submission.
	 * @param namespaces
	 */
	public abstract void initExecutable(Namespaces namespaces);

	/**
	 * Returns the set of namespaces, this execution needs to be executed on.
	 * The {@link ExecutionManager} then submits the queries to these namespaces.
	 */
	@JsonIgnore
	public abstract Set<Namespace> getRequiredNamespaces();


	@Override
	public ManagedExecutionId createId() {
		return new ManagedExecutionId(dataset, queryId);
	}

	protected void fail() {
		synchronized (execution) {
			state = ExecutionState.FAILED;
			finishTime = LocalDateTime.now();
			execution.countDown();
		}
	}

	public void start() {
		startTime = LocalDateTime.now();
		state = ExecutionState.RUNNING;
	}

	protected void finish() {
		if (getState() == ExecutionState.NEW)
			log.error("Query {} was never run.", getId());

		synchronized (execution) {
			finishTime = LocalDateTime.now();
			state = ExecutionState.DONE;
			execution.countDown();
			try {
				storage.updateExecution(this);
			}
			catch (JSONException e) {
				log.error("Failed to store {} after finishing: {}", getClass().getSimpleName(), this, e);
			}
		}

		log.info(
			"{} {} {} within {}",
			state,
			queryId,
			this.getClass().getSimpleName(),
			(startTime != null && finishTime != null) ? Duration.between(startTime, finishTime) : null);
	}

	public void awaitDone(int time, TimeUnit unit) {
		if (state == ExecutionState.RUNNING) {
			Uninterruptibles.awaitUninterruptibly(execution, time, unit);
		}
	}

	public ExecutionStatus buildStatus(URLBuilder url, User user) {
		return ExecutionStatus.builder()
							  .label(label)
							  .id(getId())
							  .own(getOwner().equals(user.getId()))
							  .createdAt(getCreationTime().atZone(ZoneId.systemDefault()))
							  .requiredTime((startTime != null && finishTime != null) ? ChronoUnit.MILLIS.between(startTime, finishTime) : null).status(state)
							  .owner(Optional.ofNullable(owner).orElse(null))
							  .ownerName(
									  Optional.ofNullable(owner).map(owner -> storage.getUser(owner)).map(User::getLabel)
											  .orElse(null))
							  .resultUrl(
									  isReadyToDownload(url, user)
									  ? url.set(ResourceConstants.DATASET, dataset.getName()).set(ResourceConstants.QUERY, getId().toString())
										   .to(ResultCSVResource.GET_CSV_PATH).get()
									  : null)
							  .build();
	}

	public boolean isReadyToDownload(URLBuilder url, User user) {
		/* We cannot rely on checking this.dataset only for download permission because the actual execution might also fired queries on another dataset.
		 * The member ManagedExecution.dataset only associates the execution with the dataset it was submitted to.
		 */
		boolean isPermittedDownload = user.isPermittedAll(getUsedNamespacedIds().stream()
			.map(NamespacedId::getDataset)
			.map(d -> DatasetPermission.onInstance(Ability.DOWNLOAD, d))
			.collect(Collectors.toList()));
		return url != null && state != ExecutionState.NEW && isPermittedDownload;
	}

	public ExecutionStatus buildStatus(User user) {
		return buildStatus( null, user);
	}

	public abstract Collection<ManagedQuery> toResultQuery();
	
	/**
	 * Gives all {@link NamespacedId}s that were required in the execution.
	 * @return A List of all {@link NamespacedId}s needed for the execution.
	 */
	@JsonIgnore
	public abstract Set<NamespacedId> getUsedNamespacedIds();
	
	
	public abstract Map<ManagedExecutionId,QueryPlan> createQueryPlans(QueryPlanContext context);

	public abstract void addResult(R result);
	
	@JsonIgnore
	public abstract R getInitializedShardResult(Entry<ManagedExecutionId, QueryPlan> entry);
	
	@JsonIgnore
	public abstract SubmittedQuery getSubmitted();
}
