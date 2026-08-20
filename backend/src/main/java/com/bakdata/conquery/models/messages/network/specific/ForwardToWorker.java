package com.bakdata.conquery.models.messages.network.specific;

import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ShardNodeNetworkContext;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @implNote Messages are sent serialized and only deserialized when they are being processed. This ensures that messages that were sent just shortly before to setup state later messages depend upon is correct.
 * @implNote Messages are additionally sent gzipped, to avoid hogging memory with long queues.
 */
@CPSType(id = "FORWARD_TO_WORKER", base = NetworkMessage.class)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString(of = {"workerId", "message"})
@EqualsAndHashCode
public class ForwardToWorker extends MessageToShardNode implements SlowMessage {

	private final WorkerId workerId;
	private final WorkerMessage message;

	@Getter(onMethod_ = @JsonIgnore(false))
	private final boolean slowMessage;

	@JsonIgnore
	@Setter
	private ProgressReporter progressReporter;

	public static ForwardToWorker create(WorkerId worker, WorkerMessage message) {
		return new ForwardToWorker(worker, message, true);
	}

	@Override
	public void react(ShardNodeNetworkContext context) throws Exception {
		final Worker worker = Objects.requireNonNull(context.getWorkers().getWorker(workerId));

		getMessage().setProgressReporter(progressReporter);
		getMessage().react(worker);
	}

}
