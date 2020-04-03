package com.bakdata.conquery.models.messages.network.specific;

import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.Master;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.Wait;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="UPDATE_SLAVE_IDENTITY", base=NetworkMessage.class)
@AllArgsConstructor @NoArgsConstructor @Getter @Setter
@Slf4j
public class RegisterWorker extends MasterMessage {

	private WorkerInformation info;
	
	@Override
	public void react(Master context) throws Exception {
		SlaveInformation slave = getSlave(context);
		Wait
			.builder()
			.attempts(6)
			.stepTime(1)
			.stepUnit(TimeUnit.SECONDS)
			.build()
			.until(()->getSlave(context));
		
		if(slave == null) {
			throw new IllegalStateException("Could not find the slave "+context.getRemoteAddress()+" to register worker "+info.getId());
		}

		if(context.getNamespaces().get(info.getDataset()) == null){
			throw new IllegalStateException("Could not find the Dataset[" + info.getDataset() + "] to register worker " + info.getId());
		}

		log.info("Received new {} for {}", info, slave);

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
