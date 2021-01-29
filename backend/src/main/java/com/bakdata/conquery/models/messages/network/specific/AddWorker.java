package com.bakdata.conquery.models.messages.network.specific;

import java.io.File;
import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ShardNodeNetworkContext;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="ADD_WORKER", base=NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator) @Getter @Slf4j
public class AddWorker extends MessageToShardNode.Slow {

	private final Dataset dataset;
	
	@Override
	public void react(ShardNodeNetworkContext context) throws Exception {
		log.info("creating a new worker for {}", dataset);
		ConqueryConfig config = context.getConfig();

		Worker worker = context.getWorkers().createWorker(dataset, config.getStorage(), context.getStoragePrefix(), createWorkerName(), context.getValidator());

		worker.setSession(context.getRawSession());
		
		context.send(new RegisterWorker(worker.getInfo()));
	}

	private String createWorkerName() {
		return "worker_"+dataset.getName()+"_"+UUID.randomUUID().toString();
	}
}
