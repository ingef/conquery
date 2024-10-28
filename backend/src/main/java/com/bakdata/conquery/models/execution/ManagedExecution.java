package com.bakdata.conquery.models.execution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.execution.OverviewExecutionStatus;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.error.ConqueryErrorInfo;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
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
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties("state")
public abstract class ManagedExecution extends IdentifiableImpl<ManagedExecutionId> implements Taggable, Shareable, Labelable, Owned, Visitable {

	/**
	 * Some unusual suffix. Its not too bad if someone actually uses this.
	 */
	public static final String AUTO_LABEL_SUFFIX = "\t@§$";

	private DatasetId dataset;
	private UUID queryId;
	private String label;

	private LocalDateTime creationTime = LocalDateTime.now();

	private UserId owner;

	@NotNull
	private String[] tags = ArrayUtils.EMPTY_STRING_ARRAY;
	private boolean shared = false;

	// Most queries contain dates, and this retroactively creates a saner default than false for old queries.
	@JsonProperty(defaultValue = "true")
	private boolean containsDates;

	@JsonAlias("machineGenerated")
	private boolean system;

	// TODO may transfer these to the ExecutionManager
	@EqualsAndHashCode.Exclude
	private LocalDateTime startTime;
	@EqualsAndHashCode.Exclude
	private LocalDateTime finishTime;
	@EqualsAndHashCode.Exclude
	private Float progress;

	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private transient ConqueryErrorInfo error;
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private transient boolean initialized = false;

	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@Getter
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private transient ConqueryConfig config;

	/**
	 * TODO remove this when identifiables hold reference to meta storage (CentralRegistry removed)
	 */
	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@Getter(AccessLevel.PROTECTED)
	@JsonIgnore
	@NotNull
	private transient MetaStorage metaStorage;

	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@Getter(AccessLevel.PROTECTED)
	@JsonIgnore
	@NotNull
	private transient DatasetRegistry<?> datasetRegistry;


	public ManagedExecution(@NonNull UserId owner, @NonNull DatasetId dataset, MetaStorage metaStorage, DatasetRegistry<?> datasetRegistry, ConqueryConfig config) {
		this.owner = owner;
		this.dataset = dataset;
		this.config = config;
		this.metaStorage = metaStorage;
		this.datasetRegistry = datasetRegistry;
	}

	private static boolean canSubjectExpand(Subject subject, QueryDescription query) {
		NamespacedIdentifiableCollector namespacesIdCollector = new NamespacedIdentifiableCollector();
		query.visit(namespacesIdCollector);

		final Set<Concept<?>> concepts = namespacesIdCollector.getIdentifiables()
															  .stream()
															  .filter(ConceptElement.class::isInstance)
															  .map(ConceptElement.class::cast)
															  .<Concept<?>>map(ConceptElement::getConcept)
															  .collect(Collectors.toSet());

		boolean canExpand = subject.isPermittedAll(concepts, Ability.READ);
		return canExpand;
	}

	/**
	 * Executed right before execution submission.
	 */
	public final void initExecutable() {

		synchronized (this) {
			if (initialized) {
				log.trace("Execution {} was already initialized", getId());
				return;
			}
			if (label == null) {
				// IdMapper is not necessary here
				label = makeAutoLabel(new PrintSettings(true, I18n.LOCALE.get(), getNamespace(), config, null, null));
			}

			doInitExecutable();

			// This can be quite slow, so setting this in overview is not optimal for users with a lot of queries.
			containsDates = containsDates(getSubmitted());

			initialized = true;
		}
	}

	protected String makeAutoLabel(PrintSettings cfg) {
		return makeDefaultLabel(cfg) + AUTO_LABEL_SUFFIX;
	}

	@JsonIgnore
	public Namespace getNamespace() {
		return datasetRegistry.get(getDataset());
	}

	protected abstract void doInitExecutable();

	private static boolean containsDates(QueryDescription query) {
		return Visitable.stream(query)
						.anyMatch(visitable -> {

							if (visitable instanceof CQConcept cqConcept) {
								return !cqConcept.isExcludeFromTimeAggregation();
							}

							if (visitable instanceof CQExternal external) {
								return external.containsDates();
							}

							return false;
						});
	}

	/**
	 * Returns the {@link QueryDescription} that caused this {@link ManagedExecution}.
	 */
	@JsonIgnore
	public abstract QueryDescription getSubmitted();

	@JsonIgnore
	protected abstract String makeDefaultLabel(PrintSettings cfg);

	@Override
	public ManagedExecutionId createId() {
		if (queryId == null) {
			queryId = UUID.randomUUID();
		}
		ManagedExecutionId managedExecutionId = new ManagedExecutionId(dataset, queryId);
		managedExecutionId.setMetaStorage(getMetaStorage());
		return managedExecutionId;
	}

	/**
	 * Fails the execution and log the occurred error.
	 */
	public void fail(ConqueryErrorInfo error) {
		if (this.error != null && !this.error.equalsRegardingCodeAndMessage(error)) {
			// Warn only again if the error is different (failed might by called per collected result)
			log.warn("The execution [{}] failed again with:\n\t{}\n\tThe previous error was: {}", getId(), this.error, error);
		}
		else {
			this.error = error;
			// Log the error, so its id is at least once in the logs
			log.warn("The execution [{}] failed with:\n\t{}", getId(), getError());
		}

		finish(ExecutionState.FAILED);
	}

	public synchronized void finish(ExecutionState executionState) {

		// Modify state
		finishTime = LocalDateTime.now();
		progress = null;

		// Set execution state before acting on the latch (to prevent a race condition - should not happen as the CachedStore uses softValues)
		getExecutionManager().updateState(getId(), executionState);

		// Persist state of this execution
		metaStorage.updateExecution(this);

		// Signal to waiting threads that the execution finished
		getExecutionManager().clearBarrier(getId());

		log.info("{} {} {} within {}", executionState, getId(), getClass().getSimpleName(), getExecutionTime());
	}

	@JsonIgnore
	protected ExecutionManager getExecutionManager() {
		return getNamespace().getExecutionManager();
	}

	@JsonIgnore
	public Duration getExecutionTime() {
		return (startTime != null && finishTime != null) ? Duration.between(startTime, finishTime) : null;
	}

	public void start() {
		synchronized (this) {
			Preconditions.checkArgument(isInitialized(), "The execution must have been initialized first");

			if (getExecutionManager().isResultPresent(getId())) {
				Preconditions.checkArgument(getExecutionManager().getResult(getId()).getState() != ExecutionState.RUNNING);
			}

			startTime = LocalDateTime.now();

			getMetaStorage().updateExecution(this);
		}
	}

	/**
	 * Renders a lightweight status with meta information about this query. Computation an size should be small for this.
	 */
	public OverviewExecutionStatus buildStatusOverview(Subject subject) {
		OverviewExecutionStatus status = new OverviewExecutionStatus();
		setStatusBase(subject, status);

		return status;
	}

	public void setStatusBase(@NonNull Subject subject, @NonNull ExecutionStatus status) {
		status.setLabel(label == null ? queryId.toString() : getLabelWithoutAutoLabelSuffix());
		status.setPristineLabel(label == null || queryId.toString().equals(label) || isAutoLabeled());
		status.setId(getId());
		status.setTags(tags);
		status.setShared(shared);
		status.setOwn(subject.isOwner(this));
		status.setCreatedAt(getCreationTime().atZone(ZoneId.systemDefault()));
		status.setRequiredTime((startTime != null && finishTime != null) ? ChronoUnit.MILLIS.between(startTime, finishTime) : null);
		status.setStartTime(startTime);
		status.setFinishTime(finishTime);
		status.setStatus(getState());
		status.setContainsDates(containsDates);

		if (owner != null) {
			User user = metaStorage.get(owner);

			if(user != null) {
				status.setOwner(user.getId());
				status.setOwnerName(user.getLabel());
			}
		}
	}

	@JsonIgnore
	public String getLabelWithoutAutoLabelSuffix() {
		final int idx;
		if (label != null && (idx = label.lastIndexOf(AUTO_LABEL_SUFFIX)) != -1) {
			return label.substring(0, idx);
		}
		return label;
	}

	@JsonIgnore
	public boolean isAutoLabeled() {
		return label != null && label.endsWith(AUTO_LABEL_SUFFIX);
	}

	public ExecutionState getState() {
		if (!getExecutionManager().isResultPresent(getId())) {
			return ExecutionState.NEW;
		}

		return getExecutionManager().getResult(getId()).getState();
	}

	/**
	 * Renders an extensive status of this query (see {@link FullExecutionStatus}. The rendering can be computation intensive and can produce a large
	 * object. The use  of the full status is only intended if a client requested specific information about this execution.
	 */
	public FullExecutionStatus buildStatusFull(Subject subject, Namespace namespace) {

		FullExecutionStatus status = new FullExecutionStatus();
		setStatusFull(status, subject, namespace);

		return status;
	}

	public void setStatusFull(FullExecutionStatus status, Subject subject, Namespace namespace) {
		setStatusBase(subject, status);

		setAdditionalFieldsForStatusWithColumnDescription(subject, status);
		setAdditionalFieldsForStatusWithSource(subject, status, namespace);
		setAdditionalFieldsForStatusWithGroups(status);
		setAvailableSecondaryIds(status);
		status.setProgress(progress);


		if (getState().equals(ExecutionState.FAILED) && error != null) {
			// Use plain format here to have a uniform serialization.
			status.setError(error.asPlain());
		}
	}

	private void setAvailableSecondaryIds(FullExecutionStatus status) {
		final QueryUtils.AvailableSecondaryIdCollector secondaryIdCollector = new QueryUtils.AvailableSecondaryIdCollector();

		visit(secondaryIdCollector);
		// TODO may work with ids directly here instead of resolving
		status.setAvailableSecondaryIds(secondaryIdCollector.getIds());
	}

	private void setAdditionalFieldsForStatusWithGroups(FullExecutionStatus status) {
		/* Calculate which groups can see this query.
		 * This is usually not done very often and should be reasonable fast, so don't cache this.
		 */
		List<GroupId> permittedGroups = new ArrayList<>();

		try(Stream<Group> allGroups = getMetaStorage().getAllGroups()) {
			for (Group group : allGroups.toList()) {
				for (Permission perm : group.getPermissions()) {
					if (perm.implies(createPermission(Ability.READ.asSet()))) {
						permittedGroups.add(group.getId());
					}
				}
			}
		}

		status.setGroups(permittedGroups);
	}

	protected void setAdditionalFieldsForStatusWithColumnDescription(Subject subject, FullExecutionStatus status) {
		// Implementation specific
	}

	/**
	 * Sets additional fields of an {@link ExecutionStatus} when a more specific status is requested.
	 */
	protected void setAdditionalFieldsForStatusWithSource(Subject subject, FullExecutionStatus status, Namespace namespace) {
		QueryDescription query = getSubmitted();

		status.setCanExpand(canSubjectExpand(subject, query));


		status.setQuery(canSubjectExpand(subject, query) ? getSubmitted() : null);
	}

	@JsonIgnore
	public boolean isReadyToDownload() {
		return getState() == ExecutionState.DONE;
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return ExecutionPermission.onInstance(abilities, getId());
	}
}
