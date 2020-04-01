package com.bakdata.conquery.models.query.results;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
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
	private ListenableFuture<List<EntityResult>> future;
	
	public synchronized void addResult(EntityResult result) {
		results.add(result);
	}

	public synchronized void finish() {
		if(finishTime == null) {
			try {
				results = new ArrayList<>(Uninterruptibles.getUninterruptibly(future));
				finishTime = LocalDateTime.now();
				log.info("Finished query {} with {} results within {}", queryId, results.size(), Duration.between(startTime, finishTime));
			} catch (ExecutionException e) {
				log.error("Failed query "+queryId, e);
			}
		}
	}

	public synchronized void send(MessageSender<NamespaceMessage> session) {
		finish();
		session.send(new CollectQueryResult(this));
	}
}
