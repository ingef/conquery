package com.bakdata.conquery.commands;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.validation.Validator;
import javax.ws.rs.client.Client;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.PathParamInjector;
import com.bakdata.conquery.io.jackson.serializer.SerdesTarget;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.mina.BinaryJacksonCoder;
import com.bakdata.conquery.io.mina.CQProtocolCodecFilter;
import com.bakdata.conquery.io.mina.ChunkReader;
import com.bakdata.conquery.io.mina.ChunkWriter;
import com.bakdata.conquery.io.mina.MinaAttributes;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
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
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.admin.AdminServlet;
import com.bakdata.conquery.resources.admin.ShutdownTask;
import com.bakdata.conquery.resources.unprotected.AuthServlet;
import com.bakdata.conquery.tasks.PermissionCleanupTask;
import com.bakdata.conquery.tasks.QueryCleanupTask;
import com.bakdata.conquery.tasks.ReportConsistencyTask;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Central node of Conquery. Hosts the frontend, api, meta data and takes care of query distribution to
 * {@link ShardNode}s and respectively the {@link Worker}s hosted on them. The {@link ManagerNode} can also
 * forward queries or results to statistic backends. Finally, it collects the results of queries for access over the api.
 */
@Slf4j
@Getter
public class ManagerNode extends IoHandlerAdapter implements Managed {

	public static final String DEFAULT_NAME = "manager";

	private final String name;

	private IoAcceptor acceptor;
	private MetaStorage storage;
	private JobManager jobManager;
	private Validator validator;
	private ConqueryConfig config;
	private AdminServlet admin;
	private AuthorizationController authController;
	private ScheduledExecutorService maintenanceService;
	private DatasetRegistry datasetRegistry;
	private Environment environment;
	private final List<ResourcesProvider> providers = new ArrayList<>();
	private Client client;

	// Resources without authentication
	private DropwizardResourceConfig unprotectedAuthApi;
	private DropwizardResourceConfig unprotectedAuthAdmin;

	// For registering form providers
	private FormScanner formScanner;

	public ManagerNode() {
		this(DEFAULT_NAME);
	}

	public ManagerNode(@NonNull String name) {
		this.name = name;
	}

	public void run(ConqueryConfig config, Environment environment) throws InterruptedException {
		this.environment = environment;
		validator = environment.getValidator();

		client = new JerseyClientBuilder(environment).using(config.getJerseyClient())
													 .build(getName());

		// Instantiate DatasetRegistry and MetaStorage so they are ready for injection into the object mapper (API + Storage)
		// The validator is already injected at this point see Conquery.java
		datasetRegistry = new DatasetRegistry(config.getCluster().getEntityBucketSize());
		storage = new MetaStorage(config.getStorage(), datasetRegistry);


		final ObjectMapper objectMapper = environment.getObjectMapper();
		customizeApiObjectMapper(objectMapper);


		jobManager = new JobManager("ManagerNode", config.isFailOnError());
		formScanner = new FormScanner();
		this.config = config;

		config.initialize(this);

		// Initialization of internationalization
		I18n.init();

		final DropwizardResourceConfig resourceConfig = environment.jersey().getResourceConfig();
		configureApiServlet(config, resourceConfig);

		maintenanceService = environment.lifecycle()
										.scheduledExecutorService("Maintenance Service")
										.build();

		environment.lifecycle().manage(this);

		loadNamespaces();

		loadMetaStorage();

		authController = new AuthorizationController(storage, config.getAuthorizationRealms());
		environment.lifecycle().manage(authController);

		unprotectedAuthAdmin = AuthServlet.generalSetup(environment.metrics(), config, environment.admin(), objectMapper);
		unprotectedAuthApi = AuthServlet.generalSetup(environment.metrics(), config, environment.servlets(), objectMapper);

		// Create AdminServlet first to make it available to the realms
		admin = new AdminServlet(this);

		authController.externalInit(this, config.getAuthenticationRealms());


		// Register default components for the admin interface
		admin.register(this);

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
						config.getQueries().getOldQueriesTime().getQuantity(),
						config.getQueries().getOldQueriesTime().getUnit().toChronoUnit()
				)));
		environment.admin().addTask(new PermissionCleanupTask(storage));
		environment.admin().addTask(new ReportConsistencyTask(datasetRegistry));

		ShutdownTask shutdown = new ShutdownTask();
		environment.admin().addTask(shutdown);
		environment.lifecycle().addServerLifecycleListener(shutdown);
	}


	private void configureApiServlet(ConqueryConfig config, DropwizardResourceConfig resourceConfig) {
		RESTServer.configure(config, resourceConfig);
		resourceConfig.register(PathParamInjector.class);
		resourceConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(datasetRegistry).to(DatasetRegistry.class);
			}
		});
	}

	/**
	 * Customize the mapper from the environment, that is used in the REST-API.
	 * In contrast to the internal object mapper this uses textual JSON representation
	 * instead of the binary smile format. It also does not expose internal fields through serialization.
	 * <p>
	 * Internal and external mapper have in common that they might process the same classes/objects and that
	 * they are configured to understand certain Conquery specific data types.
	 *
	 * @param objectMapper to be configured (should be a JSON mapper)
	 */
	public void customizeApiObjectMapper(ObjectMapper objectMapper) {
		objectMapper.setConfig(objectMapper.getDeserializationConfig().withAttribute(SerdesTarget.class, SerdesTarget.MANAGER));

		final MutableInjectableValues injectableValues = new MutableInjectableValues();
		objectMapper.setInjectableValues(injectableValues);
		injectableValues.add(Validator.class, getValidator());

		getDatasetRegistry().injectInto(objectMapper);
		getStorage().injectInto(objectMapper);
	}

	/**
	 * Create a new internal object mapper for binary (de-)serialization that is equipped with {@link ManagerNode} related injectables
	 * and configured to use the {@link InternalOnly} view.
	 * <p>
	 * TODO we need to distinguish between internal persistence and internal communication (manager<->shard). ATM we persist unnecessary fields.
	 *
	 * @return a preconfigured binary object mapper
	 * @see ManagerNode#customizeApiObjectMapper(ObjectMapper)
	 */
	public ObjectMapper createInternalObjectMapper() {
		final ObjectMapper objectMapper = getConfig().configureObjectMapper(Jackson.BINARY_MAPPER.copy());


		final MutableInjectableValues injectableValues = new MutableInjectableValues();
		objectMapper.setInjectableValues(injectableValues);
		injectableValues.add(Validator.class, getValidator());
		getDatasetRegistry().injectInto(objectMapper);
		getStorage().injectInto(objectMapper);

		objectMapper.setConfig(objectMapper.getDeserializationConfig().withAttribute(SerdesTarget.class, SerdesTarget.MANAGER));
		objectMapper.setConfig(objectMapper.getDeserializationConfig().withView(InternalOnly.class));
		objectMapper.setConfig(objectMapper.getSerializationConfig().withView(InternalOnly.class));
		return objectMapper;
	}

	private void loadMetaStorage() {
		log.info("Opening MetaStorage");
		storage.openStores(createInternalObjectMapper());
		log.info("Loading MetaStorage");
		storage.loadData();
		log.info("MetaStorage loaded {}", storage);

		datasetRegistry.setMetaStorage(storage);
	}

	@SneakyThrows(InterruptedException.class)
	public void loadNamespaces() {


		Queue<Namespace> namespacesDone = new ConcurrentLinkedQueue<>();
		ExecutorService loaders = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		// Namespaces load their storage themselves, so they can inject Namespace relevant objects into stored objects
		final Collection<NamespaceStorage> namespaceStorages = config.getStorage().discoverNamespaceStorages();
		for (NamespaceStorage namespaceStorage : namespaceStorages) {
			loaders.submit(() -> {
				namespacesDone.add(Namespace.createAndRegister(getDatasetRegistry(), namespaceStorage, getConfig(), createInternalObjectMapper()));
			});
		}


		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {
			log.debug("Waiting for Worker namespaces to load. {} are already finished. {} pending.", namespacesDone.size(), namespaceStorages.size()
																															- namespacesDone.size());
		}
	}

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
		if (message instanceof MessageToManagerNode) {
			MessageToManagerNode mrm = (MessageToManagerNode) message;
			log.trace("ManagerNode received {} from {}", message.getClass().getSimpleName(), session.getRemoteAddress());

			ReactingJob<MessageToManagerNode, NetworkMessageContext.ManagerNodeNetworkContext>
					job =
					new ReactingJob<>(mrm, new NetworkMessageContext.ManagerNodeNetworkContext(
							jobManager,
							new NetworkSession(session),
							datasetRegistry, config.getCluster().getBackpressure()
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
		}
	}

	@Override
	public void start() throws Exception {
		acceptor = new NioSocketAcceptor();

		ObjectMapper om = createInternalObjectMapper();
		config.configureObjectMapper(om);
		BinaryJacksonCoder coder = new BinaryJacksonCoder(datasetRegistry, validator, om);
		acceptor.getFilterChain().addLast("codec", new CQProtocolCodecFilter(new ChunkWriter(coder), new ChunkReader(coder, om)));
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
		}
		catch (Exception e) {
			log.error(acceptor + " could not be closed", e);
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

		client.close();
	}
}
