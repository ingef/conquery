package com.bakdata.conquery.models.messages.network.specific;

import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ManagerNodeNetworkContext;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.Wait;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@CPSType(id="REGISTER_SHARD_WORKER_IDENTITY", base=NetworkMessage.class)
@AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class RegisterWorker extends MessageToManagerNode {

	private WorkerInformation info;
	
	@Override
	public void react(ManagerNodeNetworkContext context) throws Exception {
		ShardNodeInformation node = getShardNode(context);
		Wait
			.builder()
			.attempts(6)
			.stepTime(1)
			.stepUnit(TimeUnit.SECONDS)
			.build()
			.until(()->getShardNode(context));
		
		if(node == null) {
			throw new IllegalStateException("Could not find the slave "+context.getRemoteAddress()+" to register worker "+info.getId());
		}

		info.setConnectedShardNode(node);
		context.getNamespaces().register(node, info);
	}

	/**
	 * Utility method to get the slave information from the context.
	 * @param context the network context
	 * @return the found slave or null if none was found
	 */
	private ShardNodeInformation getShardNode(ManagerNodeNetworkContext context) {
		return context.getNamespaces()
			.getShardNodes()
			.get(context.getRemoteAddress());
	}
}
