package com.bakdata.conquery.models.messages.network.specific;

import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.Master;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.google.common.util.concurrent.Uninterruptibles;

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
		SlaveInformation slave = getSlave(context);
		for(int attempt = 0; attempt < 6 && slave == null; attempt++) {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			slave = getSlave(context);
		}
		
		if(slave == null) {
			throw new IllegalStateException("Could not find the slave "+context.getRemoteAddress()+" to register worker "+info.getId());
		}
		info.setConnectedSlave(slave);
		context.getNamespaces().register(slave, info);
	}

	/**
	 * Utility method to get the slave information from the context.
	 * @param context the network context
	 * @return the found slave or null if none was found
	 */
	private SlaveInformation getSlave(Master context) {
		return context.getNamespaces()
			.getSlaves()
			.get(context.getRemoteAddress());
	}
}
