package com.bakdata.conquery.mode.cluster;

import java.net.InetSocketAddress;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import com.bakdata.conquery.models.worker.ShardWorkers;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.WorkerInformation;
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
	private final ShardWorkers workers;
	private final InternalMapperFactory internalMapperFactory;

	private JobManager jobManager;
	private ScheduledExecutorService scheduler;
	private NioSocketConnector connector;
	private ConnectFuture future;
	private NetworkMessageContext.ShardNodeNetworkContext context;

	@Override
	public void sessionCreated(IoSession session) {
		log.debug("Session created: {}", session);
	}


	@Override
	public void sessionOpened(IoSession session) {
		NetworkSession networkSession = new NetworkSession(session, config.getCluster().getNetworkSessionMaxQueueLength());

		// Schedule ShardNode and Worker registration, so we don't block this thread which does the actual sending
		scheduler.schedule(() -> {
			context = new NetworkMessageContext.ShardNodeNetworkContext(networkSession, workers, config, environment);
			log.info("Connected to ManagerNode @ `{}`", session.getRemoteAddress());

			// Authenticate with ManagerNode
			context.send(new AddShardNode());

			for (Worker w : workers.getWorkers().values()) {
				w.setSession(networkSession);
				WorkerInformation info = w.getInfo();
				log.info("Sending worker identity '{}'", info.getName());
				networkSession.send(new RegisterWorker(info));
			}
		}, 0, TimeUnit.SECONDS);


		scheduleIdleLogger(scheduler, session, config.getCluster().getIdleTimeOut());
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

	@Override
	public void sessionClosed(IoSession session) {
		log.info("Disconnected from ManagerNode.");

		try {
			scheduler.schedule(this::connectToCluster, 2, TimeUnit.SECONDS);
		}
		catch (RejectedExecutionException e) {
			log.trace("Scheduler rejected execution (probably in shutdown). Skipping reconnect attempt", e);
		}
	}

	private void connectToCluster() {
		final InetSocketAddress address = new InetSocketAddress(
				config.getCluster().getManagerURL().getHostAddress(),
				config.getCluster().getPort()
		);

		disconnectFromCluster();

		connector = getClusterConnector();

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
	private NioSocketConnector getClusterConnector() {
		ObjectMapper om = internalMapperFactory.createShardCommunicationMapper();

		return config.getCluster().getClusterConnector(om, this, "Shard");
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

	@Override
	public void start() throws Exception {


		jobManager = new JobManager(environment.getName(), config.isFailOnError());

		scheduler = environment.lifecycle().scheduledExecutorService("cluster-connection-shard").build();
		// Connect async as the manager might not be up jet or is started by a test in succession
		scheduler.schedule(this::connectToCluster, 0, TimeUnit.MINUTES);

		scheduler.scheduleAtFixedRate(this::reportJobManagerStatus, 30, 1, TimeUnit.SECONDS);

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
	public void stop() throws Exception {
		// close scheduler before disconnect to avoid scheduled reconnects
		scheduler.shutdown();
		disconnectFromCluster();
		jobManager.close();
	}

	public boolean isBusy() {
		return jobManager.isSlowWorkerBusy();
	}
}
