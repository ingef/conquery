package com.bakdata.conquery.models.messages.network.specific;

import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ShardNodeNetworkContext;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

@CPSType(id="FORWARD_TO_WORKER", base=NetworkMessage.class)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED) @Getter
public class ForwardToWorker extends MessageToShardNode implements SlowMessage {

	private static final ObjectWriter WRITER = Jackson.BINARY_MAPPER.copy().writerFor(WorkerMessage.class).withView(InternalOnly.class);

	@SneakyThrows // TODO check if this can kill whole threads.
	public static ForwardToWorker create(WorkerId worker, WorkerMessage message) {
		return new ForwardToWorker(
				worker,
				WRITER.writeValueAsBytes(message),
				message.isSlowMessage(),
				message.toString()
		);
	}

	private final WorkerId workerId;
	private final byte[] messageRaw;

	// We cache this on the sender side.
	@Getter(onMethod_ = @JsonIgnore(false))
	private final boolean slowMessage;
	private final String text;

	@JsonIgnore
	@Setter
	private ProgressReporter progressReporter;

	@Override
	public void react(ShardNodeNetworkContext context) throws Exception {
		Worker worker = Objects.requireNonNull(context.getWorkers().getWorker(workerId));
		ConqueryMDC.setLocation(worker.toString());

		WorkerMessage message = context.getWorkers()
									   .getBinaryMapper()
									   .readerFor(WorkerMessage.class)
									   .withView(InternalOnly.class)
									   .readValue(messageRaw);

		if(message instanceof SlowMessage){
			((SlowMessage) message).setProgressReporter(progressReporter);
		}

		message.react(worker);
	}


	@Override
	public String toString() {
		return messageRaw.toString() + " for worker " + workerId;
	}
}
