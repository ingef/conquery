package com.bakdata.conquery.commands;

import java.io.File;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.validation.Validator;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.mina.BinaryJacksonCoder;
import com.bakdata.conquery.io.mina.CQProtocolCodecFilter;
import com.bakdata.conquery.io.mina.ChunkReader;
import com.bakdata.conquery.io.mina.ChunkWriter;
import com.bakdata.conquery.io.mina.MinaAttributes;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.MetaStorageImpl;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.ReactingJob;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.admin.AdminServlet;
import com.bakdata.conquery.resources.admin.ShutdownTask;
import com.bakdata.conquery.resources.unprotected.AuthServlet;
import com.bakdata.conquery.tasks.ClearFilterSourceSearch;
import com.bakdata.conquery.tasks.QueryCleanupTask;
import com.bakdata.conquery.tasks.ReportConsistencyTask;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.google.common.base.Throwables;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * Central node of Conquery. Hosts the frontend, api, meta data and takes care of query distribution to 
 * {@link ShardNode}s and respectively the {@link Worker}s hosted on them. The {@link ManagerNode} can also
 * forward queries or results to statistic backends. Finally it collects the results of queries for access over the api.
 */
@Slf4j
@Getter
public class ManagerNode extends IoHandlerAdapter implements Managed {

	private IoAcceptor acceptor;
	private MetaStorage storage;
	private JobManager jobManager;
	private Validator validator;
	private ConqueryConfig config;
	private AdminServlet admin;
	private AuthorizationController authController;
	private AuthServlet authServletApp;
	private AuthServlet authServletAdmin;
	private ScheduledExecutorService maintenanceService;
	private DatasetRegistry datasetRegistry;
	private Environment environment;
	private List<ResourcesProvider> providers = new ArrayList<>();

	public void run(ConqueryConfig config, Environment environment) throws InterruptedException {

		datasetRegistry = new DatasetRegistry(config.getCluster().getEntityBucketSize());

		//inject datasets into the objectmapper
		((MutableInjectableValues)environment.getObjectMapper().getInjectableValues())
				.add(IdResolveContext.class, datasetRegistry);


		this.jobManager = new JobManager("ManagerNode");
		this.environment = environment;
		this.validator = environment.getValidator();
		this.config = config;

		// Initialization of internationalization
		I18n.init();

		RESTServer.configure(config, environment.jersey().getResourceConfig());



		this.maintenanceService = environment
			.lifecycle()
			.scheduledExecutorService("Maintenance Service")
			.build();
		
		environment.lifecycle().manage(this);

		if(config.getStorage().getDirectory().mkdirs()){
			log.warn("Had to create Storage Dir at `{}`", config.getStorage().getDirectory());
		}

		log.info("Started meta storage");

		ExecutorService loaders = Executors.newFixedThreadPool(config.getStorage().getNThreads());


		for (File directory : config.getStorage().getDirectory().listFiles((file, name) -> name.startsWith("dataset_"))) {
			loaders.submit(() -> {
				NamespaceStorage datasetStorage = NamespaceStorage.tryLoad(validator, config.getStorage(), directory);

				if (datasetStorage == null) {
					log.warn("Unable to load a dataset at `{}`", directory);
					return;
				}

				Namespace ns = new Namespace(datasetStorage);
				ns.initMaintenance(maintenanceService);
				datasetRegistry.add(ns);
			});
		}


		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)){
			log.debug("Still waiting for Datasets to load. {} already finished.", datasetRegistry.getDatasets());
		}

		log.info("All stores loaded: {}", datasetRegistry.getDatasets());
		
		
		this.storage = new MetaStorageImpl(datasetRegistry, environment.getValidator(), config.getStorage());
		this.storage.loadData();
		log.info("MetaStorage loaded {}", this.storage);

		datasetRegistry.setMetaStorage(this.storage);
		for (Namespace sn : datasetRegistry.getDatasets()) {
			sn.getStorage().setMetaStorage(storage);
		}


		
		authController = new AuthorizationController(environment, config.getAuthorization(), config.getAuthentication(), storage);
		authController.init();
		environment.lifecycle().manage(authController);

		log.info("Registering ResourcesProvider");
		for (Class<? extends ResourcesProvider> resourceProvider : CPSTypeIdResolver.listImplementations(ResourcesProvider.class)) {
			try {
				ResourcesProvider provider = resourceProvider.getConstructor().newInstance();
				provider.registerResources(this);
				providers.add(provider);
			} catch (Exception e) {
				log.error("Failed to register Resource {}",resourceProvider, e);
			}
		}

		admin = new AdminServlet();
		admin.register(this);

		// Register an unprotected servlet for logins on the app port
		AuthServlet.registerUnprotectedApiResources(authController, environment.metrics(), config, environment.servlets(), environment.getObjectMapper());

		// Register an unprotected servlet for logins on the admin port
		AuthServlet.registerUnprotectedAdminResources(authController, environment.metrics(), config, environment.admin(), environment.getObjectMapper());

		Task formScanner = new FormScanner();
		try {
			formScanner.execute(null, null);
		}
		catch (Exception e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
		environment.admin().addTask(formScanner);
		environment.admin().addTask(
				new QueryCleanupTask(storage, Duration.of(
						ConqueryConfig.getInstance().getQueries().getOldQueriesTime().getQuantity(),
						ConqueryConfig.getInstance().getQueries().getOldQueriesTime().getUnit().toChronoUnit()
				)));
		environment.admin().addTask(new ClearFilterSourceSearch());
		environment.admin().addTask(new ReportConsistencyTask(datasetRegistry));

		ShutdownTask shutdown = new ShutdownTask();
		environment.admin().addTask(shutdown);
		environment.lifecycle().addServerLifecycleListener(shutdown);
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		ConqueryMDC.setLocation("ManagerNode["+session.getLocalAddress().toString()+"]");
		log.info("New client {} connected, waiting for identity", session.getRemoteAddress());
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		log.info("Client '{}' disconnected ", session.getAttribute(MinaAttributes.IDENTIFIER));
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		log.error("caught exception", cause);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		if (message instanceof MessageToManagerNode) {
			MessageToManagerNode mrm = (MessageToManagerNode) message;
			log.trace("ManagerNode received {} from {}", message.getClass().getSimpleName(), session.getRemoteAddress());
			ReactingJob<MessageToManagerNode, NetworkMessageContext.ManagerNodeNetworkContext> job = new ReactingJob<>(mrm, new NetworkMessageContext.ManagerNodeNetworkContext(
				jobManager,
				new NetworkSession(session),
				datasetRegistry
			));

			// TODO: 01.07.2020 FK: distribute messages/jobs to their respective JobManagers (if they have one)
			if (mrm.isSlowMessage()) {
				((SlowMessage) mrm).setProgressReporter(job.getProgressReporter());
				jobManager.addSlowJob(job);
			} else {
				jobManager.addFastJob(job);
			}
		} else {
			log.error("Unknown message type {} in {}", message.getClass(), message);
			return;
		}
	}

	@Override
	public void start() throws Exception {
		acceptor = new NioSocketAcceptor();

		BinaryJacksonCoder coder = new BinaryJacksonCoder(datasetRegistry, validator);
		acceptor.getFilterChain().addLast("codec", new CQProtocolCodecFilter(new ChunkWriter(coder), new ChunkReader(coder)));
		acceptor.setHandler(this);
		acceptor.getSessionConfig().setAll(config.getCluster().getMina());
		acceptor.bind(new InetSocketAddress(config.getCluster().getPort()));
		log.info("Started ManagerNode @ {}", acceptor.getLocalAddress());
	}

	@Override
	public void stop() throws Exception {
		jobManager.close();

		datasetRegistry.close();
		
		try {
			acceptor.dispose();
		} catch (Exception e) {
			log.error(acceptor + " could not be closed", e);
		}
		
		for (ResourcesProvider provider : providers) {
			try {
				provider.close();
			} catch (Exception e) {
				log.error(provider + " could not be closed", e);
			}

		}
		try {
			storage.close();
		} catch (Exception e) {
			log.error(storage + " could not be closed", e);
		}
	}
}
