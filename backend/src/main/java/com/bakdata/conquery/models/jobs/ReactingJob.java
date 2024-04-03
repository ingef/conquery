package com.bakdata.conquery.models.jobs;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor @ToString
public class ReactingJob<MESSAGE extends NetworkMessage<CTX>, CTX extends NetworkMessageContext<?>> extends Job {

	private MESSAGE message;
	@ToString.Exclude
	private CTX context;
	
	@Override
	public void execute() {
		ConqueryMDC.setLocation(message.toString());
		try {
			message.react(context);
		} catch(Exception e) {
			throw new RuntimeException("Failed while processing the message "+message, e);
		}
		finally {
			ConqueryMDC.clearLocation();
		}
	}

	@Override
	public String getLabel() {
		return "reacting to "+message;
	}
}
