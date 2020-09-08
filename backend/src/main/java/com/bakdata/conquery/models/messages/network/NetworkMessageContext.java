package com.bakdata.conquery.models.messages.network;

import javax.validation.Validator;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.Workers;

import lombok.Getter;

public abstract class NetworkMessageContext<MESSAGE extends NetworkMessage<?>> extends MessageSender.Simple<MESSAGE> {
	@Getter
	private final JobManager jobManager;
	
	public NetworkMessageContext(JobManager jobManager, NetworkSession session) {
		super(session);
		this.jobManager = jobManager;
	}
	
	public boolean isConnected() {
		return session != null;
	}

	@Getter
	public static class Slave extends NetworkMessageContext<MessageToManagerNode> {
		
		private final Workers workers;
		private final ConqueryConfig config;
		private final Validator validator;
		private NetworkSession rawSession;

		public Slave(JobManager jobManager, NetworkSession session, Workers workers, ConqueryConfig config, Validator validator) {
			super(jobManager, session);
			this.workers = workers;
			this.config = config;
			this.validator = validator;
			this.rawSession = session;
		}
	}
	
	@Getter
	public static class ManagerNodeRxTxContext extends NetworkMessageContext<SlaveMessage> {

		private final Namespaces namespaces;
		
		public ManagerNodeRxTxContext(JobManager jobManager, NetworkSession session, Namespaces namespaces) {
			super(jobManager, session);
			this.namespaces = namespaces;
		}
	}
}
