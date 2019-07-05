package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.Slave;
import com.bakdata.conquery.models.messages.network.SlaveMessage;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="REMOVE_WORKER", base=NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator) @Getter @Slf4j
public class RemoveWorker extends SlaveMessage.Slow {

	private final DatasetId dataset;
	
	@Override
	public void react(Slave context) throws Exception {
		log.info("removing worker for {}", dataset);
		context.getWorkers().removeWorkersFor(dataset);
	}
}
