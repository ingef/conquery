package com.bakdata.conquery.models.query.results;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.CollectQueryResult;
import com.bakdata.conquery.models.query.IQuery;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @Slf4j
public class ShardResult {

	private ManagedQueryId queryId;
	private IQuery query;
	private List<EntityResult> results = new ArrayList<>();
	private LocalDateTime startTime = LocalDateTime.now();
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
