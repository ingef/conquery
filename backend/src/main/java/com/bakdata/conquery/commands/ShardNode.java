package com.bakdata.conquery.commands;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.validation.Validator;

import com.bakdata.conquery.io.mina.BinaryJacksonCoder;
import com.bakdata.conquery.io.mina.CQProtocolCodecFilter;
import com.bakdata.conquery.io.mina.ChunkReader;
import com.bakdata.conquery.io.mina.ChunkWriter;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.jobs.ReactingJob;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.Message;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ShardNodeNetworkContext;
import com.bakdata.conquery.models.messages.network.specific.AddShardNode;
import com.bakdata.conquery.models.messages.network.specific.RegisterWorker;
import com.bakdata.conquery.models.messages.network.specific.UpdateJobManagerStatus;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.Workers;
import com.bakdata.conquery.util.io.ConqueryMDC;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

@Slf4j
@Getter
public class ShardNode extends ConqueryCommand implements IoHandler, Managed {

	private NioSocketConnector connector;
	private JobManager jobManager;
	private Validator validator;
	private ConqueryConfig config;
	private ShardNodeNetworkContext context;
	private Workers workers;
	@Setter
	private ScheduledExecutorService scheduler;

	public ShardNode() {
		this("shard-node");
	}

	public ShardNode(String name) {
		super(name, "Connects this instance as a ShardNode to a running ManagerNode.");		
	}
	



	@Override
	protected void run(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception {
		connector = new NioSocketConnector();

		jobManager = new JobManager(getName());
		synchronized (environment) {
			environment.lifecycle().manage(this);
			validator = environment.getValidator();

			scheduler = environment
								.lifecycle()
								.scheduledExecutorService("Scheduled Messages")
								.build();
		}

		scheduler.scheduleAtFixedRate(this::reportJobManagerStatus, 30, 1, TimeUnit.SECONDS);

		this.config = config;

		if (config.getStorage().getDirectory().mkdirs()) {
			log.warn("Had to create Storage Dir at `{}`", config.getStorage().getDirectory());
		}

		workers = new Workers(config.getQueries().getExecutionPool(), config.getStorage().getNThreads());
		ExecutorService loaders = Executors.newFixedThreadPool(config.getStorage().getNThreads());


		File storageDir = config.getStorage().getDirectory();
		for (File directory : storageDir.listFiles((file, name) -> name.startsWith("worker_"))) {

			loaders.submit(() -> {
				ConqueryMDC.setLocation(directory.toString());

				WorkerStorage workerStorage = WorkerStorage.tryLoad(validator, config.getStorage(), directory);
				if (workerStorage == null) {
					log.warn("No valid WorkerStorage found.");
					return;
				}

				workers.createWorker(
						workerStorage
				);

				ConqueryMDC.clearLocation();
			});
		}

		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {
			log.debug("Waiting for Workers to load. {} are already finished.", workers.getWorkers().size());
		}

		log.info("All Worker Storages loaded: {}", workers.getWorkers().size());
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		setLocation(session);
		if (message instanceof MessageToShardNode) {
			MessageToShardNode srm = (MessageToShardNode) message;
			log.trace("{} recieved {} from {}", getName(), message.getClass().getSimpleName(), session.getRemoteAddress());
			ReactingJob<MessageToShardNode, NetworkMessageContext.ShardNodeNetworkContext> job = new ReactingJob<>(srm, context);

			if (((Message) message).isSlowMessage()) {
				((SlowMessage) message).setProgressReporter(job.getProgressReporter());
				jobManager.addSlowJob(job);
			}
			else {
				jobManager.addFastJob(job);
			}
		}
		else {
			log.error("Unknown message type {} in {}", message.getClass(), message);
			return;
		}
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		setLocation(session);
		log.error("cought exception", cause);
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		setLocation(session);
		NetworkSession networkSession = new NetworkSession(session);

		context = new NetworkMessageContext.ShardNodeNetworkContext(jobManager, networkSession, workers, config, validator);
		log.info("Connected to ManagerNode @ {}", session.getRemoteAddress());

		// Authenticate with ManagerNode
		context.send(new AddShardNode());

		for (Worker w : workers.getWorkers().values()) {
			w.setSession(new NetworkSession(session));
			WorkerInformation info = w.getInfo();
			log.info("Sending worker identity '{}'", info.getName());
			networkSession.send(new RegisterWorker(info));
		}
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		setLocation(session);
		log.info("Disconnected from ManagerNode");
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
	}

	@Override
	public void inputClosed(IoSession session) throws Exception {
	}

	@Override
	public void event(IoSession session, FilterEvent event) throws Exception {
	}

	private void setLocation(IoSession session) {
		String loc = session.getLocalAddress().toString();
		ConqueryMDC.setLocation(loc);
	}

	@Override
	public void start() throws Exception {
		for (Worker value : workers.getWorkers().values()) {
			value.getJobManager().addSlowJob(new SimpleJob("Update Block Manager", value.getStorage().getBucketManager()::fullUpdate));
		}

		BinaryJacksonCoder coder = new BinaryJacksonCoder(workers, validator);
		connector.getFilterChain().addLast("codec", new CQProtocolCodecFilter(new ChunkWriter(coder), new ChunkReader(coder)));
		connector.setHandler(this);
		connector.getSessionConfig().setAll(config.getCluster().getMina());

		InetSocketAddress address = new InetSocketAddress(
				config.getCluster().getManagerURL().getHostAddress(),
				config.getCluster().getPort()
		);

		while (true) {
			try {
				log.info("Trying to connect to {}", address);

				// Try opening a connection (Note: This fails immediately instead of waiting a minute to try and connect)
				ConnectFuture future = connector.connect(address);

				future.awaitUninterruptibly();

				if (future.isConnected()) {
					break;
				}

				future.cancel();
				// Sleep thirty seconds then retry.
				TimeUnit.SECONDS.sleep(30);

			}
			catch (RuntimeIoException e) {
				log.warn("Failed to connect to " + address, e);
			}
		}
	}

	@Override
	public void stop() throws Exception {
		getJobManager().close();
		
		workers.stop();
		
		//after the close command was send
		if (context != null) {
			context.awaitClose();
		}
		log.info("Connection was closed by ManagerNode");
		connector.dispose();
	}

	private void reportJobManagerStatus() {
		if (context == null || !context.isConnected()) {
			return;
		}

		// Collect the ShardNode and all its workers jobs into a single queue
		final JobManagerStatus jobManagerStatus = jobManager.reportStatus();

		for (Worker worker : workers.getWorkers().values()) {
			jobManagerStatus.getJobs().addAll(worker.getJobManager().reportStatus().getJobs());
		}

		try {
			context.trySend(new UpdateJobManagerStatus(jobManagerStatus));
		}
		catch (Exception e) {
			log.warn("Failed to report job manager status", e);
		}
	}

	public boolean isBusy() {
		return getJobManager().isSlowWorkerBusy() || workers.isBusy();
	}
}
