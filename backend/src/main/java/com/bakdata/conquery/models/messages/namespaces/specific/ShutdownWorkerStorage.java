package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="SHUTDOWN_WORKER", base=NamespacedMessage.class) @Slf4j
@RequiredArgsConstructor(onConstructor_=@JsonCreator) @Getter
public class ShutdownWorkerStorage extends WorkerMessage {

	@Override
	public void react(Worker context) throws Exception {
		context.close();
	}}
