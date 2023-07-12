package com.bakdata.conquery.mode.cluster;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.mina.BinaryJacksonCoder;
import com.bakdata.conquery.io.mina.CQProtocolCodecFilter;
import com.bakdata.conquery.io.mina.ChunkReader;
import com.bakdata.conquery.io.mina.ChunkWriter;
import com.bakdata.conquery.io.mina.MinaAttributes;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.ReactingJob;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ShutdownShard;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * Manager of the connection from the manager to the ConQuery shards.
 */
@Slf4j
@RequiredArgsConstructor
public class ClusterConnectionManager extends IoHandlerAdapter {

	private IoAcceptor acceptor;
	private final DatasetRegistry<DistributedNamespace> datasetRegistry;
	private final JobManager jobManager;
	private final Validator validator;
	private final ConqueryConfig config;
	private final InternalObjectMapperCreator internalObjectMapperCreator;
	@Getter
	private final ClusterState clusterState;

	@Override
	public void sessionOpened(IoSession session) {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		log.info("New client {} connected, waiting for identity", session.getRemoteAddress());
	}

	@Override
	public void sessionClosed(IoSession session) {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		log.info("Client '{}' disconnected ", session.getAttribute(MinaAttributes.IDENTIFIER));
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		log.error("caught exception", cause);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		if (message instanceof MessageToManagerNode toManagerNode) {

			log.trace("ManagerNode received {} from {}", message.getClass().getSimpleName(), session.getRemoteAddress());

			Job job = new ReactingJob<>(toManagerNode,
				new NetworkMessageContext.ManagerNodeNetworkContext(
						new NetworkSession(session),
						datasetRegistry,
						clusterState,
						config.getCluster().getBackpressure()
				));

			if (toManagerNode instanceof SlowMessage slowMessage) {
				slowMessage.setProgressReporter(job.getProgressReporter());
				jobManager.addSlowJob(job);
			}
			else {
				jobManager.addFastJob(job);
			}
		}
		else {
			log.error("Unknown message type {} in {}", message.getClass(), message);
		}
	}

	public void start() throws IOException {
		acceptor = new NioSocketAcceptor();

		ObjectMapper om = internalObjectMapperCreator.createInternalObjectMapper(View.InternalCommunication.class);
		config.configureObjectMapper(om);
		BinaryJacksonCoder coder = new BinaryJacksonCoder(datasetRegistry, validator, om);
		acceptor.getFilterChain().addLast("codec", new CQProtocolCodecFilter(new ChunkWriter(coder), new ChunkReader(coder, om)));
		acceptor.setHandler(this);
		acceptor.getSessionConfig().setAll(config.getCluster().getMina());
		acceptor.bind(new InetSocketAddress(config.getCluster().getPort()));
		log.info("Started ManagerNode @ {}", acceptor.getLocalAddress());
	}

	public void stop() {
		clusterState.getShardNodes().forEach(((socketAddress, shardNodeInformation) -> shardNodeInformation.send(new ShutdownShard())));

		try {
			acceptor.dispose();
		}
		catch (RuntimeException e) {
			log.error("{} could not be closed", acceptor, e);
		}

	}
}
