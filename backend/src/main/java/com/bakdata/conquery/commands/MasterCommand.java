package com.bakdata.conquery.commands;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.validation.Validator;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.mina.BinaryJacksonCoder;
import com.bakdata.conquery.io.mina.CQProtocolCodecFilter;
import com.bakdata.conquery.io.mina.ChunkReader;
import com.bakdata.conquery.io.mina.ChunkWriter;
import com.bakdata.conquery.io.mina.MinaAttributes;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.MasterMetaStorageImpl;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.ReactingJob;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.admin.AdminServlet;
import com.bakdata.conquery.resources.admin.ShutdownTask;
import com.bakdata.conquery.resources.unprotected.AuthServlet;
import com.bakdata.conquery.tasks.QueryCleanupTask;
import com.bakdata.conquery.util.io.ConqueryMDC;
import io.dropwizard.cli.ServerCommand;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

@Slf4j
@Getter
public class MasterCommand extends ServerCommand<ConqueryConfig> implements Managed, IoHandler {

	private IoAcceptor acceptor;
	private MasterMetaStorage storage;
	private JobManager jobManager;
	private Validator validator;
	private ConqueryConfig config;
	private AdminServlet admin;
	private AuthorizationController authController;
	private AuthServlet authServletApp;
	private AuthServlet authServletAdmin;
	private ScheduledExecutorService maintenanceService;
	private Namespaces namespaces = new Namespaces();
	private Environment environment; // TODO: 27.03.2020 inline this/provide as parameter. It's not a component needed for storage
	private List<ResourcesProvider> providers = new ArrayList<>();

	public MasterCommand(Conquery conquery) {
		super(conquery, "server", "Start the master Server.");
	}

	@Override
	protected void run(Environment environment, net.sourceforge.argparse4j.inf.Namespace namespace, ConqueryConfig configuration) throws Exception {

		this.config = configuration;

		//inject namespaces into the objectmapper
		((MutableInjectableValues) environment.getObjectMapper().getInjectableValues())
				.add(NamespaceCollection.class, namespaces);


		this.jobManager = new JobManager("master");
		this.environment = environment;

		// Initialization of internationalization
		I18n.init();

		RESTServer.configure(configuration, environment.jersey().getResourceConfig());

		environment.lifecycle().manage(jobManager);

		this.validator = environment.getValidator();

		this.maintenanceService = environment
										  .lifecycle()
										  .scheduledExecutorService("Maintenance Service")
										  .build();

		environment.lifecycle().manage(this);

		log.info("Started meta storage");
		for (File directory : configuration.getStorage().getDirectory().listFiles()) {
			if (directory.getName().startsWith("dataset_")) {
				NamespaceStorage datasetStorage = NamespaceStorage.tryLoad(validator, configuration.getStorage(), directory);
				if (datasetStorage != null) {
					Namespace ns = new Namespace(datasetStorage);
					ns.initMaintenance(maintenanceService);
					namespaces.add(ns);
				}
			}
		}


		this.storage = new MasterMetaStorageImpl(namespaces, environment.getValidator(), configuration.getStorage());
		this.storage.loadData();
		namespaces.setMetaStorage(this.storage);
		for (Namespace sn : namespaces.getNamespaces()) {
			sn.getStorage().setMetaStorage(storage);
		}

		authController = new AuthorizationController(configuration.getAuthorization(), configuration.getAuthentication(), storage);
		authController.init();
		environment.lifecycle().manage(authController);

		log.info("Registering ResourcesProvider");
		for (Class<? extends ResourcesProvider> resourceProvider : CPSTypeIdResolver.listImplementations(ResourcesProvider.class)) {
			try {
				ResourcesProvider provider = resourceProvider.getConstructor().newInstance();
				provider.registerResources(this);
				providers.add(provider);
			}
			catch (Exception e) {
				log.error("Failed to register Resource {}", resourceProvider, e);
			}
		}

		admin = new AdminServlet();
		admin.register(this, authController, configuration);

		// Register an unprotected servlet for logins on the app port
		AuthServlet.registerUnprotectedApiResources(authController, environment.metrics(), configuration, environment.servlets(), environment.getObjectMapper());

		// Register an unprotected servlet for logins on the admin port
		AuthServlet.registerUnprotectedAdminResources(authController, environment.metrics(), configuration, environment.admin(), environment.getObjectMapper());

		environment.servlets().setBaseResource(this.getClass().getResource("/urlrewrite.xml").toExternalForm().replaceFirst("urlrewrite.xml",""));


		environment.admin().addTask(new QueryCleanupTask(storage));

		ShutdownTask shutdown = new ShutdownTask();
		environment.admin().addTask(shutdown);
		environment.lifecycle().addServerLifecycleListener(shutdown);

		super.run(environment, namespace, configuration);
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {

	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		ConqueryMDC.setLocation("Master[" + session.getLocalAddress().toString() + "]");
		log.info("New client {} connected, waiting for identity", session.getRemoteAddress());
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		ConqueryMDC.setLocation("Master[" + session.getLocalAddress().toString() + "]");
		log.info("Client '{}' disconnected ", session.getAttribute(MinaAttributes.IDENTIFIER));
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {

	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		ConqueryMDC.setLocation("Master[" + session.getLocalAddress().toString() + "]");
		log.error("caught exception", cause);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		ConqueryMDC.setLocation("Master[" + session.getLocalAddress().toString() + "]");
		if (message instanceof MasterMessage) {
			MasterMessage mrm = (MasterMessage) message;
			log.trace("Master received {} from {}", message.getClass().getSimpleName(), session.getRemoteAddress());
			ReactingJob<MasterMessage, NetworkMessageContext.Master> job = new ReactingJob<>(mrm, new NetworkMessageContext.Master(
					jobManager,
					new NetworkSession(session),
					namespaces
			));

			if (mrm.isSlowMessage()) {
				((SlowMessage) mrm).setProgressReporter(job.getProgressReporter());
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
	public void messageSent(IoSession session, Object message) throws Exception {

	}

	@Override
	public void inputClosed(IoSession session) throws Exception {
		session.closeNow();
	}

	@Override
	public void event(IoSession session, FilterEvent event) throws Exception {

	}

	@Override
	public void start() throws Exception {
		acceptor = new NioSocketAcceptor();

		BinaryJacksonCoder coder = new BinaryJacksonCoder(namespaces, validator);
		acceptor.getFilterChain().addLast("codec", new CQProtocolCodecFilter(new ChunkWriter(coder), new ChunkReader(coder)));
		acceptor.setHandler(this);
		acceptor.getSessionConfig().setAll(getConfig().getCluster().getMina());
		acceptor.bind(new InetSocketAddress(getConfig().getCluster().getPort()));
		log.info("Started master @ {}", acceptor.getLocalAddress());
	}

	@Override
	public void stop() throws Exception {
		log.info("Shutting down master.");

		try {
			acceptor.dispose(true);
			acceptor = null;
			log.debug("Acceptor closed.");
		}
		catch (Exception e) {
			log.error(acceptor + " could not be closed", e);
		}

		for (Namespace namespace : namespaces.getNamespaces()) {
			try {
				namespace.getStorage().close();
			}
			catch (Exception e) {
				log.error(namespace + " could not be closed", e);
			}

		}
		for (ResourcesProvider provider : providers) {
			try {
				provider.close();
			}
			catch (Exception e) {
				log.error(provider + " could not be closed", e);
			}

		}
		try {
			storage.close();
		}
		catch (Exception e) {
			log.error(storage + " could not be closed", e);
		}
	}
}
