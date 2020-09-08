package com.bakdata.conquery.models.messages.network.specific;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ManagerNodeRxTxContext;
import com.bakdata.conquery.models.worker.SlaveInformation;
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
	public void react(ManagerNodeRxTxContext context) throws Exception {
		SlaveInformation slave = context.getNamespaces()
										 .getShardNodes()
										 .get(context.getRemoteAddress());

		if (slave == null) {
			log.error("Could not find ShardNode {}, I only know of {}", context.getRemoteAddress(), context.getNamespaces().getShardNodes().keySet());
		}
		else {
			slave.setJobManagerStatus(status);
		}
	}
}
