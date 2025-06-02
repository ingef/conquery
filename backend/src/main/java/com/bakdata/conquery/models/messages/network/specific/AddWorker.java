package com.bakdata.conquery.models.messages.network.specific;

import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ShardNodeNetworkContext;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id = "ADD_WORKER", base = NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
@Slf4j
public class AddWorker extends MessageToShardNode.Slow {

	private final Dataset dataset;

	@Override
	public void react(ShardNodeNetworkContext context) throws Exception {
		log.info("creating a new worker for {}", dataset);

		dataset.setStorageProvider(context.getWorkers());

		Worker worker =
				context.getWorkers().newWorker(dataset, createWorkerName(), context.getSession(), context.getConfig().getStorage(), context.getConfig().isFailOnError());

		context.send(new RegisterWorker(worker.getInfo()));
	}

	private String createWorkerName() {
		return "worker_%s_%s".formatted(dataset.getName(), UUID.randomUUID());
	}
}
