package com.bakdata.conquery.models.execution;

import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus.CreationFlag;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.shiro.authz.Permission;

@Getter
@Setter
@ToString
@Slf4j
@CPSBase
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public abstract class ManagedExecution<R extends ShardResult> extends IdentifiableImpl<ManagedExecutionId> implements Taggable, Shareable, Labelable {

	protected DatasetId dataset;
	protected UUID queryId;
	protected String label;

	protected LocalDateTime creationTime = LocalDateTime.now();
	@Nullable
	protected UserId owner;

	@NotNull
	private String[] tags = ArrayUtils.EMPTY_STRING_ARRAY;
	private boolean shared = false;

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
	protected transient ExecutionError error;

	public ManagedExecution(UserId owner, DatasetId submittedDataset) {
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
		if(queryId == null) {
			queryId = UUID.randomUUID();
		}
		return new ManagedExecutionId(dataset, queryId);
	}

	protected void fail(MasterMetaStorage storage, ExecutionError error) {
		if(this.error != null) {
			log.warn("The execution [{}] failed again with:\n\t{}\n\tThe previously error was: {}", getId(), this.error, error);
		} else {
			this.error = error;
		}
		
		finish(storage, ExecutionState.FAILED);
	}

	public void start() {
		startTime = LocalDateTime.now();
		state = ExecutionState.RUNNING;
	}

	protected void finish(MasterMetaStorage storage, ExecutionState executionState) {
		if (getState() == ExecutionState.NEW)
			log.error("Query[{}] was never run.", getId());

		synchronized (execution) {
			finishTime = LocalDateTime.now();
			// Set execution state before acting on the latch to prevent a race condition
			// Not sure if also the storage needs an update first
			setState(executionState);
			execution.countDown();

			// No need to persist failed queries. (As they are most likely invalid)
			if(getState() == ExecutionState.DONE) {
				if(storage == null) {
					log.warn("Not saving successful execution {} because no storage was provided", getId());
					return;
				}
				try {
					storage.updateExecution(this);
				}
				catch (JSONException e) {
					log.error("Failed to store execution {} after finishing: {}", getClass().getSimpleName(), this, e);
				}
			}
		}


		log.info(
			"{} {} {} within {}",
			state,
			queryId,
			this.getClass().getSimpleName(),
			getExecutionTime()
		);
	}

	@JsonIgnore
	public Duration getExecutionTime() {
		return (startTime != null && finishTime != null) ? Duration.between(startTime, finishTime) : null;
	}

	public void awaitDone(int time, TimeUnit unit) {
		if (state == ExecutionState.RUNNING) {
			Uninterruptibles.awaitUninterruptibly(execution, time, unit);
		}
	}

	protected void setStatusBase(@NonNull MasterMetaStorage storage, URLBuilder url, @NonNull  User user, @NonNull ExecutionStatus status) {
		status.setLabel(label == null ? queryId.toString() : label);
		status.setPristineLabel(label == null || queryId.toString().equals(label));
		status.setId(getId());
		status.setTags(tags);
		status.setShared(shared);
		status.setOwn(getOwner().equals(user.getId()));
		status.setCreatedAt(getCreationTime().atZone(ZoneId.systemDefault()));
		status.setRequiredTime((startTime != null && finishTime != null) ? ChronoUnit.MILLIS.between(startTime, finishTime) : null);
		status.setStatus(state);
		status.setOwner(Optional.ofNullable(owner).orElse(null));
		status.setOwnerName(Optional.ofNullable(owner).map(owner -> storage.getUser(owner)).map(User::getLabel).orElse(null));
		status.setResultUrl(
			isReadyToDownload(url, user)
				? getDownloadURL(url)
				: null);
		if (status.equals(ExecutionState.FAILED)) {
			assert(error != null);
			status.setError(error.asPlain());
		}
	}

	/**
	 * Allows the implementation to define an specific endpoint from where the result is to be downloaded.
	 */
	protected abstract URL getDownloadURL(URLBuilder url);

	public ExecutionStatus buildStatus(@NonNull MasterMetaStorage storage, URLBuilder url, User user) {
		return buildStatus(storage, url, user, EnumSet.noneOf(ExecutionStatus.CreationFlag.class));
	}
	public ExecutionStatus buildStatus(@NonNull MasterMetaStorage storage, URLBuilder url, User user, @NonNull ExecutionStatus.CreationFlag creationFlag) {
		return buildStatus(storage, url, user, EnumSet.of(creationFlag));
	}
	
	public ExecutionStatus buildStatus(@NonNull MasterMetaStorage storage, URLBuilder url, User user, @NonNull EnumSet<ExecutionStatus.CreationFlag> creationFlags) {
		ExecutionStatus status = new ExecutionStatus();
		setStatusBase(storage, url, user, status);
		for(CreationFlag flag : creationFlags) {
			switch (flag) {
				case WITH_COLUMN_DESCIPTION:
					setAdditionalFieldsForStatusWithColumnDescription(storage, url, user, status);
					break;
				case WITH_SOURCE:
					setAdditionalFieldsForStatusWithSource(storage, url, user, status);
					break;
				default:
					throw new IllegalArgumentException(String.format("Unhandled creation flag %s", flag));
			}
		}
		return status;
		
	}

	protected void setAdditionalFieldsForStatusWithColumnDescription(@NonNull MasterMetaStorage storage, URLBuilder url, User user, ExecutionStatus status) {
		// Implementation specific
	}

	/**
	 * Sets additional fields of an {@link ExecutionStatus} when a more specific status is requested.
	 */
	protected void setAdditionalFieldsForStatusWithSource(@NonNull MasterMetaStorage storage, URLBuilder url, User user, ExecutionStatus status) {
		QueryDescription query = getSubmitted();
		NamespacedIdCollector namespacesIdCollector = new NamespacedIdCollector();
		query.visit(namespacesIdCollector);
		List<Permission> permissions = new ArrayList<>();
		QueryUtils.generateConceptReadPermissions(namespacesIdCollector, permissions);

		boolean canExpand = user.isPermittedAll(permissions);


		status.setCanExpand(canExpand);
		status.setQuery(canExpand ? getSubmitted() : null);
	}

	public boolean isReadyToDownload(URLBuilder url, User user) {
		/* We cannot rely on checking this.dataset only for download permission because the actual execution might also fired queries on another dataset.
		 * The member ManagedExecution.dataset only associates the execution with the dataset it was submitted to.
		 */
		boolean isPermittedDownload = user.isPermittedAll(getUsedNamespacedIds().stream()
			.map(NamespacedId::getDataset)
			.map(d -> DatasetPermission.onInstance(Ability.DOWNLOAD, d))
			.collect(Collectors.toList()));
		return url != null && state == ExecutionState.DONE && isPermittedDownload;
	}

	/**
	 * Provides the result of the execution directly as a {@link StreamingOutput} with is directly returned as a response to a download request.
	 * This way, no assumption towards the form/type of the result are made and the effective handling of the result is up to the implementation.
	 */
	@JsonIgnore
	public abstract StreamingOutput getResult(IdMappingState mappingState, PrintSettings settings, Charset charset, String lineSeparator);
	
	/**
	 * Gives all {@link NamespacedId}s that were required in the execution.
	 * @return A List of all {@link NamespacedId}s needed for the execution.
	 */
	@JsonIgnore
	public abstract Set<NamespacedId> getUsedNamespacedIds();


	/**
	 * Creates a mapping from subexecutions. Their id is mapped to their {@link QueryPlan}.
	 */
	public abstract Map<ManagedExecutionId,QueryPlan> createQueryPlans(QueryPlanContext context);

	public abstract void addResult(@NonNull MasterMetaStorage storage, R result);

	/**
	 * Initializes the result that is send from a worker to the Master.
	 * E.g. this function enables the {@link ManagedForm} to prepare the result in order to be
	 * matched to its subqueries.
	 */
	@JsonIgnore
	public abstract R getInitializedShardResult(Entry<ManagedExecutionId, QueryPlan> entry);

	/**
	 * Returns the {@link QueryDescription} that caused this {@link ManagedExecution}.
	 */
	@JsonIgnore
	public abstract QueryDescription getSubmitted();
}
