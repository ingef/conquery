package com.bakdata.conquery.models.messages.network.specific;

import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.Master;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.bakdata.conquery.models.worker.WorkerInformation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="UPDATE_SLAVE_IDENTITY", base=NetworkMessage.class) @Slf4j
@AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class RegisterWorker extends MasterMessage {

	private WorkerInformation info;
	
	@Override
	public void react(Master context) throws Exception {
		SlaveInformation slave = context
			.getNamespaces()
			.getSlaves()
			.get(context.getRemoteAddress());
		Objects.requireNonNull(slave);
		info.setConnectedSlave(slave);
		context.getNamespaces().register(slave, info);
	}
}
