package com.bakdata.conquery.models.messages.network.specific;

import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.jobs.SimpleJob;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;

/**
 * Messages are sent serialized and only deserialized when they are being processed. This ensures that messages that were sent just shortly before to setup state later messages depend upon is correct.
 */
@CPSType(id="FORWARD_TO_WORKER", base=NetworkMessage.class)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString(of = {"workerId", "text"})
public class ForwardToWorker extends MessageToShardNode implements SlowMessage {

	@SneakyThrows(JsonProcessingException.class)
	public static ForwardToWorker create(WorkerId worker, WorkerMessage message, ObjectWriter writer) {
		return new ForwardToWorker(
				worker,
				writer.writeValueAsBytes(message),
				message.isSlowMessage(),
				message.toString()
		);
	}

	private final WorkerId workerId;
	private final byte[] messageRaw;

	// We cache these on the sender side.
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


		// Jobception: this is to ensure that no subsequent message is deserialized before one message is processed
		worker.getJobManager().addSlowJob(new SimpleJob("Deserialize and process WorkerMessage", () -> {

			WorkerMessage message = deserializeMessage(messageRaw, context.getWorkers().getBinaryMapper());


				message.setProgressReporter(progressReporter);
				message.react(worker);
		}));
	}

	private static WorkerMessage deserializeMessage(byte[] messageRaw, ObjectMapper binaryMapper) throws java.io.IOException {
		return binaryMapper.readerFor(WorkerMessage.class)
						   .withView(InternalOnly.class)
						   .readValue(messageRaw);
	}
}
