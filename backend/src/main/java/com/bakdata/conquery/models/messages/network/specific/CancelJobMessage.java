package com.bakdata.conquery.models.messages.network.specific;

import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@CPSType(id="CANCEL_JOB", base= NetworkMessage.class)
@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class CancelJobMessage extends MessageToShardNode {

	private final UUID jobId;

	@Override
	public void react(NetworkMessageContext.ShardNodeNetworkContext context) throws Exception {
		context.getWorkers().getWorkers().forEach((id, worker) -> worker.getJobManager().cancelJob(getJobId()));
	}
}
