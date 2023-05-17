package com.bakdata.conquery.models.messages.network.specific;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ManagerNodeNetworkContext;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id = "UPDATE_JOB_MANAGER_STATUS", base = NetworkMessage.class)
@Slf4j
@Data
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class UpdateJobManagerStatus extends MessageToManagerNode {
	@NotNull
	private final JobManagerStatus status;

	@Override
	public void react(ManagerNodeNetworkContext context) throws Exception {
		final ShardNodeInformation node = context.getNamespaces().getShardNodes().get(context.getRemoteAddress());

		if (node == null) {
			log.error("Could not find ShardNode `{}`, I only know of {}", context.getRemoteAddress(), context.getNamespaces().getShardNodes().keySet());
			return;
		}
		// The shards don't know their own name so we attach it here
		node.addJobManagerStatus(status.withOrigin(context.getRemoteAddress().toString()));
	}
}
