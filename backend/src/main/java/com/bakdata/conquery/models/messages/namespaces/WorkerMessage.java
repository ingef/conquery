package com.bakdata.conquery.models.messages.namespaces;

import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class WorkerMessage extends NamespacedMessage<Worker> {

	public static abstract class Slow extends WorkerMessage implements SlowMessage {
		@JsonIgnore @Getter @Setter
		private ProgressReporter progressReporter;
	}
}
