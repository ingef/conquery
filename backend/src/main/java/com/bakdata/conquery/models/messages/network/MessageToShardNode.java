package com.bakdata.conquery.models.messages.network;

import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

public abstract class MessageToShardNode extends NetworkMessage<NetworkMessageContext.ShardNodeNetworkContext> {

	public static abstract class Slow extends MessageToShardNode implements SlowMessage {
		@JsonIgnore @Getter @Setter
		private ProgressReporter progressReporter;
	}
}
