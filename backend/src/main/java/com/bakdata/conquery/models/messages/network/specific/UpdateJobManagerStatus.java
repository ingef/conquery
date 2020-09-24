package com.bakdata.conquery.models.messages.network.specific;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ManagerNodeNetworkContext;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="UPDATE_JOB_MANAGER_STATUS", base=NetworkMessage.class)
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString(of = "status")
@Slf4j
public class UpdateJobManagerStatus extends MessageToManagerNode {
	@NotNull
	private JobManagerStatus status;

	@Override
	public void react(ManagerNodeNetworkContext context) throws Exception {
		ShardNodeInformation node = context.getNamespaces()
										 .getShardNodes()
										 .get(context.getRemoteAddress());

		if (node == null) {
			log.error("Could not find ShardNode {}, I only know of {}", context.getRemoteAddress(), context.getNamespaces().getShardNodes().keySet());
		}
		else {
			node.setJobManagerStatus(status);
		}
	}
}
