package com.bakdata.conquery.mode.cluster;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.bakdata.conquery.io.mina.BinaryJacksonCoder;
import com.bakdata.conquery.io.mina.CQProtocolCodecFilter;
import com.bakdata.conquery.io.mina.ChunkReader;
import com.bakdata.conquery.io.mina.ChunkWriter;
import com.bakdata.conquery.io.mina.MdcFilter;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.jobs.ReactingJob;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.messages.network.specific.AddShardNode;
import com.bakdata.conquery.models.messages.network.specific.RegisterWorker;
import com.bakdata.conquery.models.messages.network.specific.UpdateJobManagerStatus;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.Workers;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.jetbrains.annotations.NotNull;

/**
 * Pendant to {@link ClusterConnectionManager}
 */
@RequiredArgsConstructor
@Slf4j
public class ClusterConnectionShard implements Managed, IoHandler {

	private final ConqueryConfig config;
	private final Environment environment;
	private final Workers workers;
	private final JobManager jobManager;
	private final Supplier<ObjectMapper> communicationMapperSupplier;

	private ScheduledExecutorService scheduler;
	private NioSocketConnector connector;
	private ConnectFuture future;
	private NetworkMessageContext.ShardNodeNetworkContext context;

	@Override
	public void sessionCreated(IoSession session) {
		log.debug("Session created.");
	}


	@Override
	public void sessionOpened(IoSession session) {
		NetworkSession networkSession = new NetworkSession(session);

		context = new NetworkMessageContext.ShardNodeNetworkContext(networkSession, workers, config, environment.getValidator());
		log.info("Connected to ManagerNode @ `{}`", session.getRemoteAddress());

		// Authenticate with ManagerNode
		context.send(new AddShardNode());

		for (Worker w : workers.getWorkers().values()) {
			w.setSession(new NetworkSession(session));
			WorkerInformation info = w.getInfo();
			log.info("Sending worker identity '{}'", info.getName());
			networkSession.send(new RegisterWorker(info));
		}

		scheduleIdleLogger(scheduler, session, config.getCluster().getIdleTimeOut());
	}

	@Override
	public void sessionClosed(IoSession session) {
		log.info("Disconnected from ManagerNode.");

		scheduler.schedule(this::connectToCluster, 2, TimeUnit.SECONDS);
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {
		log.trace("Session idle {}.", status);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		log.error("Exception caught", cause);
	}


	@Override
	public void messageReceived(IoSession session, Object message) {
		if (!(message instanceof MessageToShardNode)) {
			log.error("Unknown message type {} in {}", message.getClass(), message);
			return;
		}

		log.trace("{} received {} from {}", environment.getName(), message.getClass().getSimpleName(), session.getRemoteAddress());
		ReactingJob<MessageToShardNode, NetworkMessageContext.ShardNodeNetworkContext> job = new ReactingJob<>((MessageToShardNode) message, context);

		if (message instanceof SlowMessage slowMessage) {
			slowMessage.setProgressReporter(job.getProgressReporter());
			jobManager.addSlowJob(job);
		}
		else {
			jobManager.addFastJob(job);
		}
	}


	@Override
	public void messageSent(IoSession session, Object message) {
		log.trace("Message sent: {}", message);
	}


	@Override
	public void inputClosed(IoSession session) {
		log.info("Input closed.");
		session.closeNow();
		scheduler.schedule(this::disconnectFromCluster, 0, TimeUnit.SECONDS);
	}


	@Override
	public void event(IoSession session, FilterEvent event) throws Exception {
		log.trace("Event handled: {}", event);
	}

	private void connectToCluster() {
		InetSocketAddress address = new InetSocketAddress(
				config.getCluster().getManagerURL().getHostAddress(),
				config.getCluster().getPort()
		);

		disconnectFromCluster();

		connector = getClusterConnector(workers);

		while (true) {
			try {
				log.info("Trying to connect to {}", address);

				// Try opening a connection (Note: This fails immediately instead of waiting a minute to try and connect)
				future = connector.connect(address);

				future.awaitUninterruptibly();

				if (future.isConnected()) {
					break;
				}

				future.cancel();
				// Sleep thirty seconds then retry.
				TimeUnit.SECONDS.sleep(config.getCluster().getConnectRetryTimeout().toSeconds());

			}
			catch (RuntimeIoException e) {
				log.warn("Failed to connect to {}", address, e);
			}
			catch (InterruptedException e) {
				log.warn("Interrupted while trying to connector to cluster, giving up.", e);
				break;
			}
		}
	}


	private void disconnectFromCluster() {
		if (future != null) {
			future.cancel();
		}

		//after the close command was send
		if (context != null) {
			context.awaitClose();
		}

		if (connector != null) {
			log.info("Connection was closed by ManagerNode");
			connector.dispose();
		}
	}


	@NotNull
	private NioSocketConnector getClusterConnector(IdResolveContext workers) {
		ObjectMapper om = communicationMapperSupplier.get();

		NioSocketConnector connector = new NioSocketConnector();

		BinaryJacksonCoder coder = new BinaryJacksonCoder(workers, environment.getValidator(), om);
		connector.getFilterChain().addFirst("mdc", new MdcFilter("Shard[%s]"));
		connector.getFilterChain().addLast("codec", new CQProtocolCodecFilter(new ChunkWriter(coder), new ChunkReader(coder, om)));
		connector.setHandler(this);
		connector.getSessionConfig().setAll(config.getCluster().getMina());
		return connector;
	}


	private static void scheduleIdleLogger(ScheduledExecutorService scheduler, IoSession session, Duration timeout) {
		scheduler.scheduleAtFixedRate(
				() -> {
					final Duration elapsed = Duration.milliseconds(System.currentTimeMillis() - session.getLastIoTime());
					if (elapsed.compareTo(timeout) > 0) {
						log.trace("No message sent or received since {}", elapsed);
					}
				},
				timeout.toSeconds(), timeout.toSeconds() / 2, TimeUnit.SECONDS
		);
	}


	private void reportJobManagerStatus() {
		if (context == null || !context.isConnected()) {
			return;
		}


		// Collect the ShardNode and all its workers jobs into a single queue

		for (Worker worker : workers.getWorkers().values()) {
			final JobManagerStatus jobManagerStatus = new JobManagerStatus(
					null, worker.getInfo().getDataset(),
					worker.getJobManager().getJobStatus()
			);

			try {
				context.trySend(new UpdateJobManagerStatus(jobManagerStatus));
			}
			catch (Exception e) {
				log.warn("Failed to report job manager status", e);

				if (config.isFailOnError()) {
					System.exit(1);
				}
			}
		}
	}

	@Override
	public void start() throws Exception {
		scheduler = environment.lifecycle().scheduledExecutorService("cluster-connection-shard").build();
		// Connect async as the manager might not be up jet or is started by a test in succession
		scheduler.schedule(this::connectToCluster, 0, TimeUnit.MINUTES);

		scheduler.scheduleAtFixedRate(this::reportJobManagerStatus, 30, 1, TimeUnit.SECONDS);

	}

	@Override
	public void stop() throws Exception {
		disconnectFromCluster();
		scheduler.shutdown();
	}
}
