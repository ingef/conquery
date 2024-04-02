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
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterEvent;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.util.CommonEventFilter;
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
		log.info("New client {} connected, waiting for identity", session.getRemoteAddress());

		SharedMetricRegistries.getDefault().registerAll(new ClusterMetrics(session));
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
		final DefaultIoFilterChainBuilder filterChain = acceptor.getFilterChain();
		filterChain.addFirst("mdc", new ConqueryMdcFilter(ConqueryMDC.getNode()));
		filterChain.addLast("codec", new CQProtocolCodecFilter(new ChunkWriter(coder), new ChunkReader(coder, om)));
		acceptor.setHandler(this);
		acceptor.getSessionConfig().setAll(config.getCluster().getMina());
		acceptor.bind(new InetSocketAddress(config.getCluster().getPort()));
		log.info("Started Cluster Socket @ {}", acceptor.getLocalAddress());
	}

	public void stop() {
		clusterState.getShardNodes().forEach(((socketAddress, shardNodeInformation) -> shardNodeInformation.send(new ShutdownShard())));

		try {
			for (IoSession value : acceptor.getManagedSessions().values()) {
				log.info("Closing session: {}", value);
				value.closeNow().awaitUninterruptibly();
			}

			log.info("Disposing NioSocketAcceptor: {}", acceptor);
			acceptor.unbind();
			acceptor.dispose(true);
			log.info("Disposed NioSocketAcceptor: {}", acceptor);
		}
		catch (RuntimeException e) {
			log.error("{} could not be closed", acceptor, e);
		}

	}

	@RequiredArgsConstructor
	public static class ConqueryMdcFilter extends CommonEventFilter {
		final private String node;

		private ThreadLocal<Integer> callDepth = new ThreadLocal<Integer>() {
			@Override
			protected Integer initialValue() {
				return 0;
			}
		};

		/**
		 * Adapted from {@link org.apache.mina.filter.logging.MdcInjectionFilter}
		 */
		@Override
		protected void filter(IoFilterEvent event) throws Exception {

			// since this method can potentially call into itself
			// we need to check the call depth before clearing the MDC
			int currentCallDepth = callDepth.get();
			callDepth.set(currentCallDepth + 1);

			if (currentCallDepth == 0) {
				/* copy context to the MDC when necessary. */
				ConqueryMDC.setNode(node);
				ConqueryMDC.setLocation(event.getSession().getLocalAddress().toString());
			}

			try {
				/* propagate event down the filter chain */
				event.fire();
			}
			finally {
				if (currentCallDepth == 0) {
					/* remove context from the MDC */
					ConqueryMDC.clearNode();
					ConqueryMDC.clearLocation();

					callDepth.remove();
				}
				else {
					callDepth.set(currentCallDepth);
				}
			}


		}
	}
}
