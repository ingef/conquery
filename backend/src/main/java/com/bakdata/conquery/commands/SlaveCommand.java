package com.bakdata.conquery.commands;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.validation.Validator;

import com.bakdata.conquery.models.messages.network.specific.AddSlave;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.bakdata.conquery.io.mina.BinaryJacksonCoder;
import com.bakdata.conquery.io.mina.CQProtocolCodecFilter;
import com.bakdata.conquery.io.mina.ChunkReader;
import com.bakdata.conquery.io.mina.ChunkWriter;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.ReactingJob;
import com.bakdata.conquery.models.messages.Message;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.Slave;
import com.bakdata.conquery.models.messages.network.SlaveMessage;
import com.bakdata.conquery.models.messages.network.specific.RegisterWorker;
import com.bakdata.conquery.models.messages.network.specific.UpdateJobManagerStatus;
import com.bakdata.conquery.models.query.QueryExecutor;
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

@Slf4j @Getter
public class SlaveCommand extends ConqueryCommand implements IoHandler, Managed {

	private NioSocketConnector connector;
	private JobManager jobManager;
	private Validator validator;
	private ConqueryConfig config;
	private Slave context;
	private Workers workers = new Workers();
	@Setter
	private String label = "slave";
	private ScheduledExecutorService scheduler;

	public SlaveCommand() {
		super("slave", "Connects this instance as a slave to a running master.");
	}
	

	@Override
	protected void run(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception {
		connector = new NioSocketConnector();
		jobManager = new JobManager(label);
		synchronized (environment) {
			environment.lifecycle().manage(jobManager);
			environment.lifecycle().manage(this);
			validator = environment.getValidator();
			
			scheduler = environment
				.lifecycle()
				.scheduledExecutorService("Scheduled Messages")
				.build();
		}
		
		scheduler.scheduleAtFixedRate(this::reportJobManagerStatus, 30, 1, TimeUnit.SECONDS);


		this.config = config;
		
		File storageDir = config.getStorage().getDirectory();
		for(File directory : storageDir.listFiles()) {
			if(directory.getName().startsWith("worker_")) {
				WorkerStorage workerStorage = WorkerStorage.tryLoad(validator, config.getStorage(), directory);
				if(workerStorage != null) {
					Worker worker = new Worker(
						workerStorage.getWorker(),
						jobManager,
						workerStorage,
						new QueryExecutor(config)
					);
					workers.add(worker);
				}
			}
		}
	}
	
	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		setLocation(session);
		if(message instanceof SlaveMessage) {
			SlaveMessage srm = (SlaveMessage) message;
			log.trace("Slave recieved {} from {}", message.getClass().getSimpleName(), session.getRemoteAddress());
			ReactingJob<SlaveMessage, NetworkMessageContext.Slave> job = new ReactingJob<>(srm, context);
			
			if(((Message)message).isSlowMessage()) {
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
		context = new NetworkMessageContext.Slave(jobManager, networkSession, workers, config, validator);
		log.info("Connected to master @ {}", session.getRemoteAddress());

		// Authenticate with Master
		context.send(new AddSlave());

		for(Worker w:workers.getWorkers().values()) {
			w.setSession(new NetworkSession(session));
			WorkerInformation info = w.getInfo();
			log.info("Sending worker identity '{}'", info.getName());
			networkSession.send(new RegisterWorker(info));
		}
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		setLocation(session);
		log.info("Disconnected from master");
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {}

	@Override
	public void inputClosed(IoSession session) throws Exception {}
	
	@Override
	public void event(IoSession session, FilterEvent event) throws Exception {}
	
	private void setLocation(IoSession session) {
		String loc = session.getLocalAddress().toString();
		ConqueryMDC.setLocation(loc);
	}
	
	@Override
	public void start() throws Exception {
		BinaryJacksonCoder coder = new BinaryJacksonCoder(workers, validator);
		connector.getFilterChain().addLast("codec", new CQProtocolCodecFilter(new ChunkWriter(coder), new ChunkReader(coder)));
		connector.setHandler(this);
		connector.getSessionConfig().setAll(config.getCluster().getMina());
		
		InetSocketAddress address = new InetSocketAddress(
				config.getCluster().getMasterURL().getHostAddress(),
				config.getCluster().getPort()
		);

		while(true) {
			try {
				log.info("Trying to connect to {}", address);
				ConnectFuture future = connector.connect(address);
				future.awaitUninterruptibly(1, TimeUnit.MINUTES);

				break;
			} catch(RuntimeIoException e) {
				log.warn("Failed to connect to "+address, e);
			}
		}
	}

	@Override
	public void stop() throws Exception {
		for(Worker w : new ArrayList<>(workers.getWorkers().values())) {
			try {
				w.close();
			} catch(Exception e) {
				log.error(w+" could not be closed", e);
			}
		}
		//after the close command was send
		if(context != null) {
			context.awaitClose();
		}
		log.info("Connection was closed by master");
		connector.dispose();
	}
	
	private void reportJobManagerStatus() {
		try {
			if(context!= null && context.isConnected()) {
				context.trySend(new UpdateJobManagerStatus(jobManager.reportStatus()));
			}
		} catch(Exception e) {
			log.warn("Failed to report job manager status", e);
		}
	}
}
