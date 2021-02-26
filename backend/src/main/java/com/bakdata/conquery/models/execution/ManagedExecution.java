package com.bakdata.conquery.models.execution;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.error.ConqueryErrorInfo;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.shiro.authz.Permission;
import org.jetbrains.annotations.TestOnly;

@Getter
@Setter
@ToString
@Slf4j
@CPSBase
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public abstract class ManagedExecution<R extends ShardResult> extends IdentifiableImpl<ManagedExecutionId> implements Taggable, Shareable, Labelable {
	
	/**
	 * Some unusual suffix. Its not too bad if someone actually uses this. 
	 */
	public final static String AUTO_LABEL_SUFFIX = "\t@§$";

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
	private transient ConqueryErrorInfo error;
	@JsonIgnore
	private boolean initialized = false;

	public ManagedExecution(UserId owner, DatasetId submittedDataset) {
		this.owner = owner;
		this.dataset = submittedDataset;
	}

	/**
	 * Executed right before execution submission.
	 */
	public void initExecutable(DatasetRegistry datasetRegistry, ConqueryConfig config) {
		synchronized (getExecution()) {
			if(initialized) {
				log.trace("Execution {} was already initialized", getId());
				return;
			}
			if(label == null) {
				label = makeAutoLabel(datasetRegistry, new PrintSettings(true, I18n.LOCALE.get(), datasetRegistry));
			}
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
		if(queryId == null) {
			queryId = UUID.randomUUID();
		}
		return new ManagedExecutionId(dataset, queryId);
	}

	/**
	 * Fails the execution and log the occurred error.
	 */
	protected void fail(MetaStorage storage, ConqueryErrorInfo error) {
		if(this.error != null && !this.error.equalsRegardingCodeAndMessage(error)) {
			// Warn only again if the error is different (failed might by called per collected result)
			log.warn("The execution [{}] failed again with:\n\t{}\n\tThe previous error was: {}", getId(), this.error, error);
		} else {
			this.error = error;
			// Log the error, so its id is atleast once in the logs
			log.warn("The execution [{}] failed with:\n\t{}", this.getId(), this.error);
		}
		
		finish(storage, ExecutionState.FAILED);
	}

	public void start() {
		Preconditions.checkArgument(isInitialized(), "The execution must have been initialized first");
		startTime = LocalDateTime.now();
		state = ExecutionState.RUNNING;
	}

	protected void finish(MetaStorage storage, ExecutionState executionState) {
		if (getState() == ExecutionState.NEW) {
			log.error("Query[{}] was never run.", getId());			
		}

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
				storage.updateExecution(this);
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

	@TestOnly
	public void awaitDone(int time, TimeUnit unit) {
		if (state == ExecutionState.RUNNING || state == ExecutionState.NEW) {
			Uninterruptibles.awaitUninterruptibly(execution, time, unit);
		}
	}

	protected void setStatusBase(@NonNull MetaStorage storage, @NonNull User user, @NonNull ExecutionStatus status, UriBuilder url) {
		status.setLabel(label == null ? queryId.toString() : getLabelWithoutAutoLabelSuffix());
		status.setPristineLabel(label == null || queryId.toString().equals(label) || isAutoLabeled());
		status.setId(getId());
		status.setTags(tags);
		status.setShared(shared);
		status.setOwn(owner.equals(user.getId()));
		status.setCreatedAt(getCreationTime().atZone(ZoneId.systemDefault()));
		status.setRequiredTime((startTime != null && finishTime != null) ? ChronoUnit.MILLIS.between(startTime, finishTime) : null);
		status.setStatus(state);
		status.setOwner(Optional.ofNullable(owner).orElse(null));
		status.setOwnerName(Optional.ofNullable(owner).map(owner -> storage.getUser(owner)).map(User::getLabel).orElse(null));
		status.setResultUrl(getDownloadURL(url, user).orElse(null));
	}
	

	@SneakyThrows({MalformedURLException.class, IllegalArgumentException.class, UriBuilderException.class})
	public final Optional<URL> getDownloadURL(UriBuilder url, User user) {
		if(url == null || !isReadyToDownload(url, user)) {
			// url might be null because no url was wished and no builder was provided
			return Optional.empty();
		}
		return Optional.ofNullable(getDownloadURLInternal(url));
	}

	/**
	 * Allows the implementation to define an specific endpoint from where the result is to be downloaded.
	 */
	@Nullable
	protected abstract URL getDownloadURLInternal(UriBuilder url) throws MalformedURLException, IllegalArgumentException, UriBuilderException;

	/**
	 * Renders a lightweight status with meta information about this query. Computation an size should be small for this.
	 */
	public ExecutionStatus.Overview buildStatusOverview(@NonNull MetaStorage storage, UriBuilder url, User user, DatasetRegistry datasetRegistry) {
		ExecutionStatus.Overview status = new ExecutionStatus.Overview();
		setStatusBase(storage, user, status, url);

		return status;
	}

	/**
	 * Renders an extensive status of this query (see {@link ExecutionStatus.Full}. The rendering can be computation intensive and can produce a large
	 * object. The use  of the full status is only intended if a client requested specific information about this execution.
	 */
	public ExecutionStatus.Full buildStatusFull(@NonNull MetaStorage storage, UriBuilder url, User user, DatasetRegistry datasetRegistry) {
		Preconditions.checkArgument(isInitialized(), "The execution must have been initialized first");
		ExecutionStatus.Full status = new ExecutionStatus.Full();
		setStatusBase(storage, user, status, url);

		setAdditionalFieldsForStatusWithColumnDescription(storage, url, user, status,  datasetRegistry);
		setAdditionalFieldsForStatusWithSource(storage, url, user, status);
		setAdditionalFieldsForStatusWithGroups(storage, user, status);

		if (state.equals(ExecutionState.FAILED) && error != null) {
			// Use plain format here to have a uniform serialization.
			status.setError(error.asPlain());
		}

		return status;
	}

	private void setAdditionalFieldsForStatusWithGroups(@NonNull MetaStorage storage, User user, ExecutionStatus.Full status) {
		/* Calculate which groups can see this query.
		 * This usually is usually not done very often and should be reasonable fast, so don't cache this.
		 */
		List<GroupId> permittedGroups = new ArrayList<>();
		for(Group group : storage.getAllGroups()) {
			for(Permission perm : group.getPermissions()) {
				if(perm.implies(QueryPermission.onInstance(Ability.READ, this.getId()))) {
					permittedGroups.add(group.getId());
					continue;
				}
			}
		}
		
		status.setGroups(permittedGroups);
	}

	protected void setAdditionalFieldsForStatusWithColumnDescription(@NonNull MetaStorage storage, UriBuilder url, User user, ExecutionStatus.Full status, DatasetRegistry datasetRegistry) {
		// Implementation specific
	}

	/**
	 * Sets additional fields of an {@link ExecutionStatus} when a more specific status is requested.
	 */
	protected void setAdditionalFieldsForStatusWithSource(@NonNull MetaStorage storage, UriBuilder url, User user, ExecutionStatus.Full status) {
		QueryDescription query = getSubmitted();
		NamespacedIdCollector namespacesIdCollector = new NamespacedIdCollector();
		query.visit(namespacesIdCollector);
		List<Permission> permissions = new ArrayList<>();
		QueryUtils.generateConceptReadPermissions(namespacesIdCollector, permissions);

		boolean canExpand = user.isPermittedAll(permissions);


		status.setCanExpand(canExpand);
		status.setQuery(canExpand ? getSubmitted() : null);
	}

	protected boolean isReadyToDownload(@NonNull UriBuilder url, User user) {
		if(state != ExecutionState.DONE) {
			// No url for unfinished executions, quick return
			return false;
		}

		/* We cannot rely on checking this.dataset only for download permission because the actual execution might also fired queries on another dataset.
		 * The member ManagedExecution.dataset only associates the execution with the dataset it was submitted to.
		 */
		return user.isPermittedAll(getUsedNamespacedIds().stream()
			.map(NamespacedId::getDataset)
			.map(d -> DatasetPermission.onInstance(Ability.DOWNLOAD, d))
			.collect(Collectors.toList()));
	}

	/**
	 * Provides the result of the execution directly as a {@link StreamingOutput} with is directly returned as a response to a download request.
	 * This way, no assumption towards the form/type of the result are made and the effective handling of the result is up to the implementation.
	 */
	@JsonIgnore
	public abstract StreamingOutput getResult(Function<ContainedEntityResult,ExternalEntityId> idMapper, PrintSettings settings, Charset charset, String lineSeparator);
	
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

	public abstract void addResult(@NonNull MetaStorage storage, R result);

	/**
	 * Initializes the result that is send from a worker to the ManagerNode.
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

	@JsonIgnore
	public String getLabelWithoutAutoLabelSuffix() {
		int idx;
		if(label != null && (idx = label.lastIndexOf(AUTO_LABEL_SUFFIX)) != -1){
		
			return label.substring(0, idx);
		}
		return label;
	}
	
	@JsonIgnore
	public boolean isAutoLabeled() {
		return label != null ? label.endsWith(AUTO_LABEL_SUFFIX) : false;
	}
	
	@JsonIgnore
	abstract protected void makeDefaultLabel(StringBuilder sb, DatasetRegistry datasetRegistry, PrintSettings cfg);
	
	protected String makeAutoLabel(DatasetRegistry datasetRegistry, PrintSettings cfg) {
		StringBuilder sb = new StringBuilder();
		makeDefaultLabel(sb, datasetRegistry, cfg);
		return sb.append(AUTO_LABEL_SUFFIX).toString();
	}
}
