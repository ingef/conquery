package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dummy message that is sent to ManagerNode, to authenticate the connection as a ShardNode connection.
 * This helps to avoid retaining connections from non-ShardNodes.
 */
@CPSType(id = "ADD_SHARD_NODE", base = NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Slf4j
public class AddShardNode extends MessageToManagerNode {

	@Override
	public void react(NetworkMessageContext.ManagerNodeNetworkContext context) throws Exception {
		final ShardNodeInformation nodeInformation = new ShardNodeInformation(
				new NetworkSession(context.getSession().getSession()),
				context.getBackpressure()
		);

		context.getClusterState().getShardNodes().put(context.getRemoteAddress(), nodeInformation);

		log.info("ShardNode `{}` registered.", context.getRemoteAddress());
	}
}
