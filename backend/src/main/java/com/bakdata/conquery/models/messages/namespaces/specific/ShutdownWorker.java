package com.bakdata.conquery.models.messages.namespaces.specific;

import java.io.IOException;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import lombok.extern.slf4j.Slf4j;

@CPSType(id = "SHUTDOWN_WORKER", base = NamespacedMessage.class)
@Slf4j
public class ShutdownWorker extends WorkerMessage.Slow {
	@Override
	public void react(Worker context) throws Exception {
		try {
			context.getStorage().close();
		} catch (IOException e) {
			log.error("Failed closing down worker", e);
		}
	}
}
