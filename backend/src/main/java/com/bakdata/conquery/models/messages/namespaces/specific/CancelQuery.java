package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Send message to worker to execute {@code query} on the workers associated entities.
 */
@Slf4j
@CPSType(id = "CANCEL_QUERY", base = NamespacedMessage.class)
@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class CancelQuery extends WorkerMessage {

	private final ManagedExecutionId executionId;

	@Override
	public void react(Worker context) throws Exception {
		log.debug("Cancelling Query[{}]", executionId);
		context.getQueryExecutor().cancelQuery(executionId);
	}
}
