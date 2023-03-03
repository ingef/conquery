package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@CPSType(id = "WORKER_REMOVED", base = NetworkMessage.class)
@Getter
public class AcknowledgeWorkerRemoved extends MessageToManagerNode.Slow {

	private final WorkerId workerId;

	@Override
	public void react(NetworkMessageContext.ManagerNodeNetworkContext context) throws Exception {
		context.getDatasetRegistry().acknowledgeWorkerDeletion(workerId);
	}
}
