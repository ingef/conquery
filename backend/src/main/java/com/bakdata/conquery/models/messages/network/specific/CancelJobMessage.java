package com.bakdata.conquery.models.messages.network.specific;

import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@CPSType(id="CANCEL_JOB", base= NetworkMessage.class)
public class CancelJobMessage extends MessageToShardNode {

	@Getter
	private final UUID jobId;

	@Override
	public void react(NetworkMessageContext.ShardNodeNetworkContext context) throws Exception {
		context.getWorkers().getWorkers().forEach((id, worker) -> worker.getJobManager().cancelJob(getJobId()));
	}
}
