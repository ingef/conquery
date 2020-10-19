package com.bakdata.conquery.models.messages.network.specific;

import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ShardNodeNetworkContext;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

@CPSType(id = "FORWARD_TO_WORKER", base = NetworkMessage.class)
@Getter
public class ForwardToWorker extends MessageToShardNode.Slow {

	private final WorkerId workerId;
	private final JsonNode message;

	public ForwardToWorker(WorkerId workerId, WorkerMessage workerMessage) {
		this.workerId = workerId;
		this.message = Jackson.MAPPER.valueToTree(workerMessage);
	}

	@Override
	public void react(ShardNodeNetworkContext context) throws Exception {
		Worker w = Objects.requireNonNull(context.getWorkers().getWorker(workerId));
		ConqueryMDC.setLocation(w.toString());

		final ObjectMapper objectMapper = new SingletonNamespaceCollection(
				w.getStorage().getCentralRegistry()).injectInto(w.getStorage().getDataset().injectInto(Jackson.MAPPER));
		WorkerMessage workerMessage = objectMapper
													.readValue(message.traverse(objectMapper), WorkerMessage.class);

		if(workerMessage.isSlowMessage()){
			((SlowMessage) workerMessage).setProgressReporter(getProgressReporter());
		}

		workerMessage.react(w);
	}


	@Override
	public String toString() {
		return message.toString() + " for worker " + workerId;
	}
}
