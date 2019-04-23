package com.bakdata.conquery.models.messages.network.specific;

import java.util.UUID;

import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.messages.network.SlaveMessage;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class CancelJobMessage extends SlaveMessage {

	@Getter @Setter @NonNull
	private UUID jobId;

	@Override
	public void react(NetworkMessageContext.Slave context) throws Exception {
		context.getWorkers().getWorkers().forEach((id, worker) -> worker.getJobManager().cancelJob(getJobId()));
	}
}
