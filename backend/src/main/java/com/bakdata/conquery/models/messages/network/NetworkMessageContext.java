package com.bakdata.conquery.models.messages.network;

import javax.validation.Validator;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Workers;
import lombok.Getter;

@Getter
public abstract class NetworkMessageContext<MESSAGE extends NetworkMessage<?>> extends MessageSender.Simple<MESSAGE> {
	private final JobManager jobManager;
	private final int backpressure;

	public NetworkMessageContext(JobManager jobManager, NetworkSession session, int backpressure) {
		super(session);
		this.jobManager = jobManager;
		this.backpressure = backpressure;
	}
	
	public boolean isConnected() {
		return session != null;
	}

	/**
	 * Is used on a {@link ShardNode} for sending messages to the {@link ManagerNode} and is injected into messages from the {@link ManagerNode}.
	 */
	@Getter
	public static class ShardNodeNetworkContext extends NetworkMessageContext<MessageToManagerNode> {
		
		private final Workers workers;
		private final ConqueryConfig config;
		private final Validator validator;
		private NetworkSession rawSession;

		public ShardNodeNetworkContext(JobManager jobManager, NetworkSession session, Workers workers, ConqueryConfig config, Validator validator) {
			super(jobManager, session, config.getCluster().getBackpressure());
			this.workers = workers;
			this.config = config;
			this.validator = validator;
			this.rawSession = session;
		}
	}
	
	/**
	 * Is used on a {@link ManagerNode} for sending messages to a {@link ShardNode} and is injected into messages from the {@link ShardNode}.
	 */
	@Getter
	public static class ManagerNodeNetworkContext extends NetworkMessageContext<MessageToShardNode> {

		private final DatasetRegistry namespaces;


		public ManagerNodeNetworkContext(JobManager jobManager, NetworkSession session, DatasetRegistry namespaces, int backpressure) {
			super(jobManager, session, backpressure);
			this.namespaces = namespaces;
		}
	}
}
