package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id = "SHUTDOWN_SHARD", base = NetworkMessage.class)
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
public class ShutdownShard extends MessageToShardNode.Slow {

	@Override
	public void react(NetworkMessageContext.ShardNodeNetworkContext context) throws Exception {
		new Thread("shutdown waiter thread") {
			@Override
			public void run() {
				try {
					context.getShardNode().stop();
				} catch (Exception e) {
					log.error("Failed while shutting down Shard", e);
				}
			}
		}.start();
	}
}
