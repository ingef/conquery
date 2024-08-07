package com.bakdata.conquery.commands;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.validation.Validator;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.mina.BinaryJacksonCoder;
import com.bakdata.conquery.io.mina.CQProtocolCodecFilter;
import com.bakdata.conquery.io.mina.ChunkReader;
import com.bakdata.conquery.io.mina.ChunkWriter;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.jobs.ReactingJob;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ShardNodeNetworkContext;
import com.bakdata.conquery.models.messages.network.specific.AddShardNode;
import com.bakdata.conquery.models.messages.network.specific.RegisterWorker;
import com.bakdata.conquery.models.messages.network.specific.UpdateJobManagerStatus;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.Workers;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;
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
 * This node holds a shard of data (in so called {@link Worker}s for the different datasets in conquery.
 * It delegates incomming queries to the corresponding worker and is responsible for the network communication
 * to the {@link ManagerNode}.
 */
@Slf4j
@Getter
public class ShardNode implements ConfiguredBundle<ConqueryConfig>, IoHandler, Managed {

	public static final String DEFAULT_NAME = "shard-node";

	private final String name;

	private NioSocketConnector connector;
	private ConnectFuture future;
	private JobManager jobManager;
	private Validator validator;
	private ConqueryConfig config;
	private ShardNodeNetworkContext context;
	@Setter
	private Workers workers;
	@Setter
	private ScheduledExecutorService scheduler;
	private Environment environment;

	public ShardNode() {
		this(DEFAULT_NAME);
	}

	public ShardNode(String name) {
		this.name = name;
	}


	@Override
	public void run(ConqueryConfig config, Environment environment) throws Exception {
		this.environment = environment;
		this.config = config;


		jobManager = new JobManager(getName(), config.isFailOnError());
		environment.lifecycle().manage(this);
		validator = environment.getValidator();

		scheduler = environment.lifecycle().scheduledExecutorService("Scheduled Messages").build();

		scheduler.scheduleAtFixedRate(this::reportJobManagerStatus, 30, 1, TimeUnit.SECONDS);

		workers = new Workers(
				getConfig().getQueries().getExecutionPool(),
				() -> createInternalObjectMapper(View.Persistence.Shard.class),
				() -> createInternalObjectMapper(View.InternalCommunication.class),
				getConfig().getCluster().getEntityBucketSize(),
				getConfig().getQueries().getSecondaryIdSubPlanRetention()
		);


		final Collection<WorkerStorage> workerStorages = config.getStorage().discoverWorkerStorages();


		ExecutorService loaders = config.getQueries().getExecutionPool().createService("Worker loader");

		Queue<Worker> workersDone = new ConcurrentLinkedQueue<>();
		for (WorkerStorage workerStorage : workerStorages) {
			loaders.submit(() -> {
				try {
					workersDone.add(workers.createWorker(workerStorage, config.isFailOnError()));
				}
				catch (Exception e) {
					log.error("Failed reading Storage", e);
				}
				finally {
					log.debug("DONE reading Storage {}", workerStorage);
					ConqueryMDC.clearLocation();
				}
			});
		}

		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {

			log.debug("Waiting for Worker workers to load. {} are already finished. {} pending", workersDone.size(), workerStorages.size()
																													 - workersDone.size());
		}

		log.info("All Worker loaded: {}", workers.getWorkers().size());
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

	/**
	 * Pendant to {@link ManagerNode#createInternalObjectMapper(Class)}.
	 * <p>
	 * TODO May move to {@link ConqueryCommand}
	 *
	 * @return a preconfigured binary object mapper
	 */
	public ObjectMapper createInternalObjectMapper(Class<? extends View> viewClass) {
		final ObjectMapper objectMapper = getConfig().configureObjectMapper(Jackson.copyMapperAndInjectables(Jackson.BINARY_MAPPER));

		final MutableInjectableValues injectableValues = new MutableInjectableValues();
		objectMapper.setInjectableValues(injectableValues);
		injectableValues.add(Validator.class, getValidator());


		// Set serialization config
		SerializationConfig serializationConfig = objectMapper.getSerializationConfig();

		serializationConfig = serializationConfig.withView(viewClass);

		objectMapper.setConfig(serializationConfig);

		// Set deserialization config
		DeserializationConfig deserializationConfig = objectMapper.getDeserializationConfig();

		deserializationConfig = deserializationConfig.withView(viewClass);

		objectMapper.setConfig(deserializationConfig);

		return objectMapper;
	}

	@Override
	public void messageReceived(IoSession session, Object message) {
		setLocation(session);
		if (!(message instanceof MessageToShardNode)) {
			log.error("Unknown message type {} in {}", message.getClass(), message);
			return;
		}

		log.trace("{} received {} from {}", getName(), message.getClass().getSimpleName(), session.getRemoteAddress());
		ReactingJob<MessageToShardNode, ShardNodeNetworkContext> job = new ReactingJob<>((MessageToShardNode) message, context);

		if (message instanceof SlowMessage slowMessage) {
			slowMessage.setProgressReporter(job.getProgressReporter());
			jobManager.addSlowJob(job);
		}
		else {
			jobManager.addFastJob(job);
		}
	}

	private static void setLocation(IoSession session) {
		final String loc = session.getLocalAddress().toString();
		ConqueryMDC.setLocation(loc);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		setLocation(session);
		log.error("Exception caught", cause);
	}

	@Override
	public void sessionOpened(IoSession session) {
		setLocation(session);
		NetworkSession networkSession = new NetworkSession(session);

		context = new NetworkMessageContext.ShardNodeNetworkContext(this, networkSession, workers, config, validator);
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

	private static void scheduleIdleLogger(ScheduledExecutorService scheduler, IoSession session, Duration timeout) {
		scheduler.scheduleAtFixedRate(
				() -> {
					setLocation(session);

					final Duration elapsed = Duration.milliseconds(System.currentTimeMillis() - session.getLastIoTime());
					if (elapsed.compareTo(timeout) > 0) {
						log.warn("No message sent or received since {}", elapsed);
					}
				},
				timeout.toSeconds(), timeout.toSeconds() / 2, TimeUnit.SECONDS
		);
	}

	@Override
	public void sessionClosed(IoSession session) {
		setLocation(session);
		log.info("Disconnected from ManagerNode.");

		scheduler.schedule(this::connectToCluster, 2, TimeUnit.SECONDS);
	}

	@Override
	public void sessionCreated(IoSession session) {
		setLocation(session);
		log.debug("Session created.");
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {
		setLocation(session);
		log.warn("Session idle {}.", status);
	}

	@Override
	public void messageSent(IoSession session, Object message) {
		setLocation(session);
		log.trace("Message sent: {}", message);
	}

	@Override
	public void inputClosed(IoSession session) {
		setLocation(session);
		log.info("Input closed.");
		session.closeNow();
		scheduler.schedule(this::disconnectFromCluster, 0, TimeUnit.SECONDS);
	}

	@Override
	public void event(IoSession session, FilterEvent event) throws Exception {
		setLocation(session);
		log.trace("Event handled: {}", event);
	}

	@Override
	public void start() throws Exception {
		for (Worker value : workers.getWorkers().values()) {
			value.getJobManager().addSlowJob(new SimpleJob("Update Bucket Manager", value.getBucketManager()::fullUpdate));
		}

		scheduler.schedule(this::connectToCluster, 0, TimeUnit.MINUTES);


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

	@NotNull
	private NioSocketConnector getClusterConnector(IdResolveContext workers) {
		ObjectMapper om = createInternalObjectMapper(View.InternalCommunication.class);

		NioSocketConnector connector = new NioSocketConnector();

		BinaryJacksonCoder coder = new BinaryJacksonCoder(workers, validator, om);
		connector.getFilterChain().addLast("codec", new CQProtocolCodecFilter(new ChunkWriter(coder), new ChunkReader(coder, om)));
		connector.setHandler(this);
		connector.getSessionConfig().setAll(config.getCluster().getMina());
		return connector;
	}

	@Override
	public void stop() throws Exception {
		getJobManager().close();

		workers.stop();

		disconnectFromCluster();
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

	public boolean isBusy() {
		return getJobManager().isSlowWorkerBusy() || workers.isBusy();
	}
}
