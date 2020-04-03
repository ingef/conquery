package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id = "SHUTDOWN_WORKER", base = NamespacedMessage.class)
public class ShutdownWorker extends WorkerMessage.Slow {

	@Override
	public void react(Worker context) throws Exception {
		log.info("Trying to shut down {}", context);
		context.close();
	}
}
