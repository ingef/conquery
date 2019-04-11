package com.bakdata.conquery.models.messages.namespaces;

import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class NamespaceMessage extends NamespacedMessage<Namespace> {

	public static abstract class Slow extends NamespaceMessage implements SlowMessage {
		@JsonIgnore @Getter @Setter
		private ProgressReporter progressReporter;
	}
}
