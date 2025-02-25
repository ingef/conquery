package com.bakdata.conquery.mode.cluster;

import java.io.IOException;
import jakarta.validation.Validator;

import com.bakdata.conquery.io.mina.MinaAttributes;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.ReactingJob;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.messages.network.specific.ForwardToNamespace;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

/**
 * Manager of the connection from the manager to the ConQuery shards.
 */
@Slf4j
@RequiredArgsConstructor
public class ClusterConnectionManager extends IoHandlerAdapter {

	private final DatasetRegistry<DistributedNamespace> datasetRegistry;
	private final JobManager jobManager;
	private final Validator validator;
	private final ConqueryConfig config;
	private final InternalMapperFactory internalMapperFactory;
	@Getter
	private final ClusterState clusterState;
	private IoAcceptor acceptor;

	@Override
	public void sessionOpened(IoSession session) {
		log.info("New client {} connected, waiting for identity", session.getRemoteAddress());
	}

	@Override
	public void sessionClosed(IoSession session) {
		log.info("Client '{}' disconnected ", session.getAttribute(MinaAttributes.IDENTIFIER));
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		log.error("caught exception", cause);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {
		if (!(message instanceof MessageToManagerNode toManagerNode)) {
			log.error("Unknown message type {} in {}", message.getClass(), message);
			return;

		}

		final ShardNodeInformation shardNodeInformation = clusterState.getShardNodes().get(session.getRemoteAddress());

		final NetworkSession nwSession;

		if (shardNodeInformation == null) {
			// In case the shard is not yet registered, we wont have a shardNodeInformation to pull the session from
			nwSession = new NetworkSession(session, config.getCluster().getNetworkSessionMaxQueueLength());
		}
		else {
			nwSession = shardNodeInformation.getSession();
		}

		log.trace("ManagerNode received {} from {}", message.getClass().getSimpleName(), session.getRemoteAddress());

		final Job job = new ReactingJob<>(toManagerNode, new NetworkMessageContext.ManagerNodeNetworkContext(nwSession, datasetRegistry, clusterState));

		if (toManagerNode instanceof ForwardToNamespace nsMesg) {
			datasetRegistry.get(nsMesg.getDatasetId()).getJobManager().addSlowJob(job);
		}
		else if (toManagerNode instanceof SlowMessage slowMessage) {
			slowMessage.setProgressReporter(job.getProgressReporter());
			jobManager.addSlowJob(job);
		}
		else {
			jobManager.addFastJob(job);
		}
	}

	public void start() throws IOException {
		final ObjectMapper om = internalMapperFactory.createManagerCommunicationMapper(datasetRegistry);

		acceptor = config.getCluster().getClusterAcceptor(om, this, "Manager");

		log.info("Started ManagerNode @ {}", acceptor.getLocalAddress());
	}

	public void stop() {
		try {
			acceptor.dispose();
		}
		catch (RuntimeException e) {
			log.error("{} could not be closed", acceptor, e);
		}

	}
}
