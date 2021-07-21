package com.bakdata.conquery.models.execution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.ExecutionStatus;
import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.OverviewExecutionStatus;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.error.ConqueryErrorInfo;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
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
public abstract class ManagedExecution<R extends ShardResult> extends IdentifiableImpl<ManagedExecutionId> implements Taggable, Shareable, Labelable, Owned, Visitable {

	/**
	 * Some unusual suffix. Its not too bad if someone actually uses this.
	 */
	public static final String AUTO_LABEL_SUFFIX = "\t@§$";

	@NsIdRef
	private Dataset dataset;
	private UUID queryId;
	private String label;

	private LocalDateTime creationTime = LocalDateTime.now();

	@Nullable
	@MetaIdRef
	private User owner;

	@NotNull
	private String[] tags = ArrayUtils.EMPTY_STRING_ARRAY;
	private boolean shared = false;

	private boolean machineGenerated;


	// we don't want to store or send query results or other result metadata
	@JsonIgnore
	private transient ExecutionState state = ExecutionState.NEW;
	@JsonIgnore
	private transient CountDownLatch execution;
	@JsonIgnore
	private transient LocalDateTime startTime;
	@JsonIgnore
	private transient LocalDateTime finishTime;
	@JsonIgnore
	private transient ConqueryErrorInfo error;
	@JsonIgnore
	private transient Float progress;
	@JsonIgnore
	private transient boolean initialized = false;

	@JsonIgnore
	private transient ExecutionManager executionManager;


	public ManagedExecution(User owner, Dataset dataset) {
		this.owner = owner;
		this.dataset = dataset;
	}

	/**
	 * Executed right before execution submission.
	 */
	public void initExecutable(DatasetRegistry datasetRegistry, ConqueryConfig config) {
		synchronized (this) {
			if (initialized) {
				log.trace("Execution {} was already initialized", getId());
				return;
			}
			if (label == null) {
				// IdMapper is not necessary here
				label = makeAutoLabel(new PrintSettings(true, I18n.LOCALE.get(), datasetRegistry, config, null));
			}

			executionManager = datasetRegistry.get(getDataset().getId()).getExecutionManager();

			doInitExecutable(datasetRegistry, config);
			initialized = true;
		}
	}

	protected abstract void doInitExecutable(DatasetRegistry namespaces, ConqueryConfig config);

	/**
	 * Returns the set of namespaces, this execution needs to be executed on.
	 * The {@link ExecutionManager} then submits the queries to these namespaces.
	 */
	@JsonIgnore
	public abstract Set<Namespace> getRequiredDatasets();


	@Override
	public ManagedExecutionId createId() {
		if (queryId == null) {
			queryId = UUID.randomUUID();
		}
		return new ManagedExecutionId(dataset.getId(), queryId);
	}

	/**
	 * Fails the execution and log the occurred error.
	 */
	protected void fail(MetaStorage storage, ConqueryErrorInfo error) {
		if (this.error != null && !this.error.equalsRegardingCodeAndMessage(error)) {
			// Warn only again if the error is different (failed might by called per collected result)
			log.warn("The execution [{}] failed again with:\n\t{}\n\tThe previous error was: {}", getId(), this.error, error);
		}
		else {
			this.error = error;
			// Log the error, so its id is atleast once in the logs
			log.warn("The execution [{}] failed with:\n\t{}", this.getId(), this.error);
		}

		finish(storage, ExecutionState.FAILED);
	}

	public void start() {
		synchronized (this) {
			Preconditions.checkArgument(isInitialized(), "The execution must have been initialized first");

			startTime = LocalDateTime.now();

			setState(ExecutionState.RUNNING);
			getExecutionManager().clearQueryResults(this);

			execution = new CountDownLatch(1);
		}
	}

	protected void finish(MetaStorage storage, ExecutionState executionState) {
		if (getState() == ExecutionState.NEW) {
			log.error("Query[{}] was never run.", getId());
		}

		synchronized (this) {
			finishTime = LocalDateTime.now();
			progress = null;
			// Set execution state before acting on the latch to prevent a race condition
			// Not sure if also the storage needs an update first
			setState(executionState);
			execution.countDown();

			// No need to persist failed queries. (As they are most likely invalid)
			if (getState() == ExecutionState.DONE) {
				if (storage == null) {
					log.warn("Not saving successful execution {} because no storage was provided", getId());
					return;
				}
				storage.updateExecution(this);
			}
		}


		log.info(
				"{} {} {} within {}",
				getState(),
				queryId,
				this.getClass().getSimpleName(),
				getExecutionTime()
		);
	}

	@JsonIgnore
	public Duration getExecutionTime() {
		return (startTime != null && finishTime != null) ? Duration.between(startTime, finishTime) : null;
	}

	/**
	 * Blocks until a execution finished of the specified timeout is reached. Return immediately if the execution is not running
	 */
	public void awaitDone(int time, TimeUnit unit) {
		if (getState() != ExecutionState.RUNNING) {
			return;
		}
		Uninterruptibles.awaitUninterruptibly(execution, time, unit);
	}

	protected void setStatusBase(@NonNull User user, @NonNull ExecutionStatus status, UriBuilder url, Map<DatasetId, Set<Ability>> datasetAbilities) {
		status.setLabel(label == null ? queryId.toString() : getLabelWithoutAutoLabelSuffix());
		status.setPristineLabel(label == null || queryId.toString().equals(label) || isAutoLabeled());
		status.setId(getId());
		status.setTags(tags);
		status.setShared(shared);
		status.setOwn(user.isOwner(this));
		status.setCreatedAt(getCreationTime().atZone(ZoneId.systemDefault()));
		status.setRequiredTime((startTime != null && finishTime != null) ? ChronoUnit.MILLIS.between(startTime, finishTime) : null);
		status.setStatus(getState());
		if (owner != null) {
			status.setOwner(owner.getId());
			status.setOwnerName(owner.getLabel());
		}
	}

	/**
	 * Renders a lightweight status with meta information about this query. Computation an size should be small for this.
	 */
	public OverviewExecutionStatus buildStatusOverview(UriBuilder url, User user, Map<DatasetId, Set<Ability>> datasetAbilities) {
		OverviewExecutionStatus status = new OverviewExecutionStatus();
		setStatusBase(user, status, url, datasetAbilities);

		return status;
	}

	/**
	 * Renders an extensive status of this query (see {@link FullExecutionStatus}. The rendering can be computation intensive and can produce a large
	 * object. The use  of the full status is only intended if a client requested specific information about this execution.
	 */
	public FullExecutionStatus buildStatusFull(@NonNull MetaStorage storage, UriBuilder url, User user, DatasetRegistry datasetRegistry, Map<DatasetId, Set<Ability>> datasetAbilities) {
		Preconditions.checkArgument(isInitialized(), "The execution must have been initialized first");
		FullExecutionStatus status = new FullExecutionStatus();
		setStatusBase(user, status, url, datasetAbilities);

		setAdditionalFieldsForStatusWithColumnDescription(storage, url, user, status, datasetRegistry);
		setAdditionalFieldsForStatusWithSource(user, status);
		setAdditionalFieldsForStatusWithGroups(storage, status);
		setAvailableSecondaryIds(status);
		status.setProgress(progress);


		if (getState().equals(ExecutionState.FAILED) && error != null) {
			// Use plain format here to have a uniform serialization.
			status.setError(error.asPlain());
		}

		return status;
	}

	private void setAvailableSecondaryIds(FullExecutionStatus status) {
		final QueryUtils.AvailableSecondaryIdCollector secondaryIdCollector = new QueryUtils.AvailableSecondaryIdCollector();

		visit(secondaryIdCollector);

		status.setAvailableSecondaryIds(secondaryIdCollector.getIds());
	}

	private void setAdditionalFieldsForStatusWithGroups(@NonNull MetaStorage storage, FullExecutionStatus status) {
		/* Calculate which groups can see this query.
		 * This usually is usually not done very often and should be reasonable fast, so don't cache this.
		 */
		List<GroupId> permittedGroups = new ArrayList<>();
		for (Group group : storage.getAllGroups()) {
			for (Permission perm : group.getPermissions()) {
				if (perm.implies(createPermission(Ability.READ.asSet()))) {
					permittedGroups.add(group.getId());
					continue;
				}
			}
		}

		status.setGroups(permittedGroups);
	}

	protected void setAdditionalFieldsForStatusWithColumnDescription(@NonNull MetaStorage storage, UriBuilder url, User user, FullExecutionStatus status, DatasetRegistry datasetRegistry) {
		// Implementation specific
	}

	/**
	 * Sets additional fields of an {@link ExecutionStatus} when a more specific status is requested.
	 */
	protected void setAdditionalFieldsForStatusWithSource(User user, FullExecutionStatus status) {
		QueryDescription query = getSubmitted();
		NamespacedIdentifiableCollector namespacesIdCollector = new NamespacedIdentifiableCollector();
		query.visit(namespacesIdCollector);

		final Set<Concept> concepts = namespacesIdCollector.getIdentifiables()
														   .stream()
														   .filter(ConceptElement.class::isInstance)
														   .map(ConceptElement.class::cast)
														   .map(ConceptElement::getConcept)
														   .collect(Collectors.toSet());

		boolean canExpand = user.isPermittedAll(concepts, Ability.READ);

		status.setCanExpand(canExpand);
		status.setQuery(canExpand ? getSubmitted() : null);
	}

	public boolean isReadyToDownload(Map<DatasetId, Set<Ability>> datasetAbilities) {
		if (getState() != ExecutionState.DONE) {
			// No url for unfinished executions, quick return
			return false;
		}

		//TODO this is no longer the case.

		/* We cannot rely on checking this.dataset only for download permission because the actual execution might also fired queries on another dataset.
		 * The member ManagedExecution.dataset only associates the execution with the dataset it was submitted to.
		 */

		return getUsedNamespacedIds().stream()
									 .map(NamespacedIdentifiable::getDataset)
									 .distinct()
									 .allMatch((id) -> datasetAbilities.get(id.getId()).contains(Ability.DOWNLOAD));
	}

	/**
	 * Gives all {@link NamespacedId}s that were required in the execution.
	 *
	 * @return A List of all {@link NamespacedId}s needed for the execution.
	 */
	@JsonIgnore
	public abstract Set<NamespacedIdentifiable<?>> getUsedNamespacedIds();


	public abstract void addResult(@NonNull MetaStorage storage, R result);

	/**
	 * Returns the {@link QueryDescription} that caused this {@link ManagedExecution}.
	 */
	@JsonIgnore
	public abstract QueryDescription getSubmitted();

	@JsonIgnore
	public String getLabelWithoutAutoLabelSuffix() {
		int idx;
		if (label != null && (idx = label.lastIndexOf(AUTO_LABEL_SUFFIX)) != -1) {

			return label.substring(0, idx);
		}
		return label;
	}

	@JsonIgnore
	public boolean isAutoLabeled() {
		return label != null && label.endsWith(AUTO_LABEL_SUFFIX);
	}

	@JsonIgnore
	protected abstract String makeDefaultLabel(PrintSettings cfg);

	protected String makeAutoLabel(PrintSettings cfg) {
		return makeDefaultLabel(cfg) +  AUTO_LABEL_SUFFIX;
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return ExecutionPermission.onInstance(abilities, getId());
	}

	public abstract WorkerMessage createExecutionMessage();

	public void reset() {
		setState(ExecutionState.NEW);

		executionManager.clearQueryResults(this);
	}
}
