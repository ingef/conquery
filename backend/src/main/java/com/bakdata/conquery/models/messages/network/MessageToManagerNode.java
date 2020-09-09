package com.bakdata.conquery.models.messages.network;

import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

public abstract class MessageToManagerNode extends NetworkMessage<NetworkMessageContext.ManagerNodeNetworkContext> {

	public static abstract class Slow extends MessageToManagerNode implements SlowMessage {
		@JsonIgnore @Getter @Setter
		private ProgressReporter progressReporter;
	}
}
