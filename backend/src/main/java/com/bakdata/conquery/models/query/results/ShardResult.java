package com.bakdata.conquery.models.query.results;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.CollectQueryResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
@CPSType(id = "SHARD_RESULT", base = ShardResult.class)
@Getter @Setter @Slf4j @ToString(onlyExplicitlyIncluded = true)
public class ShardResult {

	@ToString.Include
	private ManagedExecutionId queryId;

	private List<EntityResult> results = new ArrayList<>();
	
	@ToString.Include
	private LocalDateTime startTime = LocalDateTime.now();
	@ToString.Include
	private LocalDateTime finishTime;
	@JsonIgnore
	private ListenableFuture<List<Optional<EntityResult>>> future;

	private Optional<ConqueryError> error = Optional.empty();

	@ToString.Include
	private WorkerId workerId;

	public synchronized void finish() {
		if (finishTime != null) {
			return;
		}
		
		if (error.isPresent()) {
			setFinishTime();
			return;
		}

		try {
			results =  Uninterruptibles.getUninterruptibly(future).stream()
					.flatMap(Optional::stream)
					.collect(Collectors.toList());
		} catch (ConqueryError e) {
			error = Optional.of(e);
		} catch (Exception e) {
			error = Optional.of(new ConqueryError.UnknownError(e));
		}
		setFinishTime();
	}

	private void setFinishTime() {
		finishTime = LocalDateTime.now();
		log.info("Query {} finished {} with {} results within {}", queryId, error.isEmpty()? "successful" : "faulty", results.size(), Duration.between(startTime, finishTime));
	}

	public synchronized void send(MessageSender<NamespaceMessage> session) {
		session.send(new CollectQueryResult(this));
	}
}
