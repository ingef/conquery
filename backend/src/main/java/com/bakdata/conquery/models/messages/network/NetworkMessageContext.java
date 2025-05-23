package com.bakdata.conquery.models.messages.network;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.ShardWorkers;
import io.dropwizard.core.setup.Environment;
import lombok.Getter;
import lombok.NonNull;

@Getter
public abstract class NetworkMessageContext<MESSAGE extends NetworkMessage<?>> extends MessageSender.Simple<MESSAGE> {


	public NetworkMessageContext(@NonNull NetworkSession session) {
		super(session);
	}

	public boolean isConnected() {
		return session != null;
	}

	/**
	 * Is used on a {@link ShardNode} for sending messages to the {@link ManagerNode} and is injected into messages from the {@link ManagerNode}.
	 */
	@Getter
	public static class ShardNodeNetworkContext extends NetworkMessageContext<MessageToManagerNode> {

		private final ShardWorkers workers;
		private final ConqueryConfig config;
		private final Environment environment;
		private final NetworkSession rawSession;

		public ShardNodeNetworkContext(NetworkSession session, ShardWorkers workers, ConqueryConfig config, Environment environment) {
			super(session);
			this.workers = workers;
			this.config = config;
			this.environment = environment;
			this.rawSession = session;
		}
	}
	
	/**
	 * Is used on a {@link ManagerNode} for sending messages to a {@link ShardNode} and is injected into messages from the {@link ShardNode}.
	 */
	@Getter
	public static class ManagerNodeNetworkContext extends NetworkMessageContext<MessageToShardNode> {

		private final ClusterState clusterState;
		private final DatasetRegistry<DistributedNamespace> datasetRegistry;


		public ManagerNodeNetworkContext(NetworkSession session, DatasetRegistry<DistributedNamespace> datasetRegistry, ClusterState clusterState) {
			super(session);
			this.datasetRegistry = datasetRegistry;
			this.clusterState = clusterState;
		}
	}
}
