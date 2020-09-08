package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dummy message that is sent to ManagerNode, to authenticate the connection as a Slave connection.
 * This helps to avoids retaining connections from non-slaves.
 */
@CPSType(id="ADD_SLAVE", base=NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator) @Getter @Slf4j
public class AddSlave extends MessageToManagerNode {

	@Override
	public void react(NetworkMessageContext.ManagerNodeRxTxContext context) throws Exception {
		context.getNamespaces().getSlaves().put(context.getRemoteAddress(), new SlaveInformation(new NetworkSession(context.getSession().getSession())));

		log.info("Slave {} registered.", context.getRemoteAddress());
	}
}
