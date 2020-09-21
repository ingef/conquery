package com.bakdata.conquery.models.messages.network.specific;

import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ShardNodeNetworkContext;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@CPSType(id="FORWARD_TO_WORKER", base=NetworkMessage.class)
@RequiredArgsConstructor @Getter
public class ForwardToWorker extends MessageToShardNode implements SlowMessage {

	private final WorkerId workerId;
	private final WorkerMessage message;
	
	@Override
	public void react(ShardNodeNetworkContext context) throws Exception {
		Worker w = Objects.requireNonNull(context.getWorkers().getWorker(workerId));
		ConqueryMDC.setLocation(w.toString());
		message.react(w);
	}

	@Override
	public boolean isSlowMessage() {
		return message.isSlowMessage();
	}

	@Override
	public ProgressReporter getProgressReporter() {
		return ((SlowMessage)message).getProgressReporter();
	}

	@Override
	public void setProgressReporter(ProgressReporter progressReporter) {
		((SlowMessage)message).setProgressReporter(progressReporter);
	}
	
	@Override
	public String toString() {
		return message.toString()+" for worker "+workerId;
	}
}
