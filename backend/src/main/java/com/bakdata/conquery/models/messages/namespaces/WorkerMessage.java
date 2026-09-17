package com.bakdata.conquery.models.messages.namespaces;

import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/**
 * WorkerMessages are always slow to ensure that they are processed in order and that there are no conflict
 * when ids are resolved.
 */
@Getter @Setter
public abstract class WorkerMessage extends NamespacedMessage<Worker>  implements SlowMessage {
	@JsonIgnore @Getter @Setter
	private ProgressReporter progressReporter;
}
