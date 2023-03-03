package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.specific.AcknowledgeWorkerRemoved;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ShardNodeNetworkContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="REMOVE_WORKER", base=NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator) @Getter @Slf4j
public class RemoveWorker extends MessageToShardNode.Slow {

	@NsIdRef
	private final Dataset dataset;
	
	@Override
	public void react(ShardNodeNetworkContext context) throws Exception {
		log.info("Removing worker {}", dataset);

		final WorkerId workerId = context.getWorkers().removeWorkerFor(dataset.getId());

		context.send(new AcknowledgeWorkerRemoved(workerId));
	}
}
