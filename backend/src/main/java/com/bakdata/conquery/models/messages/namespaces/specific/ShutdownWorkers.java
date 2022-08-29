package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id = "SHUTDOWN_WORKERS", base = NetworkMessage.class)
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
public class ShutdownWorkers extends MessageToShardNode.Slow {

	@Override
	public void react(NetworkMessageContext.ShardNodeNetworkContext context) throws Exception {
		context.getWorkers().getWorkers().forEach((id, worker) -> {
			log.info("Closing Worker {}", id);
			worker.close();
		});
	}
}
