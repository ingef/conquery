package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.network.MasterMessage;
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
@CPSType(id = "REMOVE_SLAVE", base = NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
@Slf4j
public class RemoveSlave extends MasterMessage {

	@Override
	public void react(NetworkMessageContext.Master context) throws Exception {
		log.info("Slave {} unregistered.", context.getRemoteAddress());
		context.getNamespaces().getSlaves().remove(context.getRemoteAddress());
	}
}
