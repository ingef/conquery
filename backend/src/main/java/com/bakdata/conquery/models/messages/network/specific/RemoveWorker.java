package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ShardNodeNetworkContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="REMOVE_WORKER", base=NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Data
@Slf4j
public class RemoveWorker extends MessageToShardNode.Slow {

	private final DatasetId dataset;
	
	@Override
	public void react(ShardNodeNetworkContext context) throws Exception {
		log.info("Removing worker {}", dataset);

		context.getWorkers().removeWorkerFor(dataset);

	}
}
