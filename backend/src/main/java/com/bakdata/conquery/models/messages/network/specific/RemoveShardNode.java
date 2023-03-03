package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Message that allows salves to safely disconnect from ManagerNode node.
 *
 * @deprecated Doesn't do much at this moment (and is not stable at this moment), as the data is distributed ahead of time, but it can later be used.
 */
@Deprecated
@CPSType(id = "REMOVE_SHARD_NODE", base = NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
@Slf4j
public class RemoveShardNode extends MessageToManagerNode {

	@Override
	public void react(NetworkMessageContext.ManagerNodeNetworkContext context) throws Exception {
		log.info("ShardNode {} unregistered.", context.getRemoteAddress());
		context.getDatasetRegistry().getShardNodes().remove(context.getRemoteAddress());
	}
}
