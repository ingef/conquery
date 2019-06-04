package com.bakdata.conquery.models.execution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.apiv1.ResourceConstants;
import com.bakdata.conquery.apiv1.ResultCSVResource;
import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Getter
@Setter
@ToString
@Slf4j
@CPSBase
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
public abstract class ManagedExecution extends IdentifiableImpl<ManagedExecutionId> {

	protected DatasetId dataset;
	protected UUID queryId = UUID.randomUUID();
	@NotEmpty
	protected String label = queryId.toString();
	protected LocalDateTime creationTime = LocalDateTime.now();
	@Nullable
	protected UserId owner;
	
	//we don't want to store or send query results or other result metadata
	@JsonIgnore
	protected transient ExecutionState state = ExecutionState.RUNNING;
	@JsonIgnore
	protected transient CountDownLatch execution;
	@JsonIgnore
	protected transient LocalDateTime startTime = LocalDateTime.now();
	@JsonIgnore
	protected transient LocalDateTime finishTime;
	@JsonIgnore
	protected transient Namespace namespace;

	public ManagedExecution(Namespace namespace, UserId owner) {
		this.owner = owner;
		initExecutable(namespace);
	}

	public void initExecutable(Namespace namespace) {
		this.namespace = namespace;
		this.execution = new CountDownLatch(1);
		this.dataset = namespace.getStorage().getDataset().getId();
	}

	@Override
	public ManagedExecutionId createId() {
		return new ManagedExecutionId(dataset, queryId);
	}

	protected void finish() {
		finishTime = LocalDateTime.now();
		state = ExecutionState.DONE;
		execution.countDown();
		try {
			namespace.getStorage().getMetaStorage().updateExecution(this);
		}
		catch (JSONException e) {
			log.error("Failed to store {} after finishing: {}", this.getClass().getSimpleName(), e, this);
		}
		log.info("Finished {} {} within {}", queryId, this.getClass().getSimpleName(), Duration.between(startTime, finishTime));
	}

	public void awaitDone(int time, TimeUnit unit) {
		Uninterruptibles.awaitUninterruptibly(execution, time, unit);
	}
	
	public ExecutionStatus buildStatus(URLBuilder url) {
		return ExecutionStatus
			.builder()
			.label(label)
			.id(getId())
			.own(true)
			.createdAt(getCreationTime().atZone(ZoneId.systemDefault()))
			.requiredTime((startTime != null && finishTime != null)
				? ChronoUnit.MILLIS.between(startTime, finishTime)
				: null)
			.status(state)
			.owner(Optional.ofNullable(owner).orElse(null))
			.ownerName(Optional.ofNullable(owner).map(user -> namespace.getStorage().getMetaStorage().getUser(user).getLabel()).orElse(null))
			.resultUrl(
				url != null
				? url
					.set(ResourceConstants.DATASET, dataset.getName())
					.set(ResourceConstants.QUERY, getId().toString())
					.to(ResultCSVResource.GET_CSV_PATH).get()
				: null
			)
			.build();
	}

	public ExecutionStatus buildStatus() {
		return buildStatus(null);
	}
}
