package com.bakdata.conquery.commands;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.validation.Validator;
import javax.ws.rs.client.Client;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.PathParamInjector;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.Manager;
import com.bakdata.conquery.mode.StorageHandler;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.admin.AdminServlet;
import com.bakdata.conquery.resources.admin.ShutdownTask;
import com.bakdata.conquery.resources.unprotected.AuthServlet;
import com.bakdata.conquery.tasks.PermissionCleanupTask;
import com.bakdata.conquery.tasks.QueryCleanupTask;
import com.bakdata.conquery.tasks.ReloadMetaStorageTask;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.google.common.base.Throwables;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.glassfish.jersey.internal.inject.AbstractBinder;

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

	private Validator validator;
	private AdminServlet admin;
	private AuthorizationController authController;
	private ScheduledExecutorService maintenanceService;
	private final List<ResourcesProvider> providers = new ArrayList<>();
	private Client client;
	@Delegate(excludes = Managed.class)
	private Manager manager;

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

	public void run(Manager manager) throws InterruptedException {
		Environment environment = manager.getEnvironment();
		ConqueryConfig config = manager.getConfig();
		validator = environment.getValidator();

		client = new JerseyClientBuilder(environment).using(config.getJerseyClient())
													 .build(getName());

		this.manager = manager;

		final ObjectMapper objectMapper = environment.getObjectMapper();
		customizeApiObjectMapper(objectMapper);


		// FormScanner needs to be instantiated before plugins are initialized
		formScanner = new FormScanner(config);


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

		authController = new AuthorizationController(getStorage(), config.getAuthorizationRealms());
		environment.lifecycle().manage(authController);

		unprotectedAuthAdmin = AuthServlet.generalSetup(environment.metrics(), config, environment.admin(), objectMapper);
		unprotectedAuthApi = AuthServlet.generalSetup(environment.metrics(), config, environment.servlets(), objectMapper);

		// Create AdminServlet first to make it available to the realms
		admin = new AdminServlet(this);

		authController.externalInit(this, config.getAuthenticationRealms());


		// Register default components for the admin interface
		admin.register();

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
				new QueryCleanupTask(getStorage(), Duration.of(
						config.getQueries().getOldQueriesTime().getQuantity(),
						config.getQueries().getOldQueriesTime().getUnit().toChronoUnit()
				)));
		environment.admin().addTask(new PermissionCleanupTask(getStorage()));
		manager.getAdminTasks().forEach(environment.admin()::addTask);
		environment.admin().addTask(new ReloadMetaStorageTask(getStorage()));

		final ShutdownTask shutdown = new ShutdownTask();
		environment.admin().addTask(shutdown);
		environment.lifecycle().addServerLifecycleListener(shutdown);
	}

	private void configureApiServlet(ConqueryConfig config, DropwizardResourceConfig jerseyConfig) {
		RESTServer.configure(config, jerseyConfig);
		jerseyConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(getStorage()).to(MetaStorage.class);
				bind(getDatasetRegistry()).to(DatasetRegistry.class);
			}
		});

		jerseyConfig.register(PathParamInjector.class);
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

		// Set serialization config
		SerializationConfig serializationConfig = objectMapper.getSerializationConfig();

		serializationConfig = serializationConfig.withView(View.Api.class);

		objectMapper.setConfig(serializationConfig);

		// Set deserialization config
		DeserializationConfig deserializationConfig = objectMapper.getDeserializationConfig();

		deserializationConfig = deserializationConfig.withView(View.Api.class);

		objectMapper.setConfig(deserializationConfig);

		final MutableInjectableValues injectableValues = new MutableInjectableValues();
		objectMapper.setInjectableValues(injectableValues);
		injectableValues.add(Validator.class, getValidator());

		getDatasetRegistry().injectInto(objectMapper);
		getStorage().injectInto(objectMapper);
		getConfig().injectInto(objectMapper);
	}

	/**
	 * Create a new internal object mapper for binary (de-)serialization that is equipped with {@link ManagerNode} related injectables.
	 *
	 * @return a preconfigured binary object mapper
	 * @see ManagerNode#customizeApiObjectMapper(ObjectMapper)
	 */
	public ObjectMapper createInternalObjectMapper(Class<? extends View> viewClass) {
		return getInternalObjectMapperCreator().createInternalObjectMapper(viewClass);
	}

	private void loadMetaStorage() {
		log.info("Opening MetaStorage");
		getStorage().openStores(getInternalObjectMapperCreator().createInternalObjectMapper(View.Persistence.Manager.class));
		log.info("Loading MetaStorage");
		getStorage().loadData();
		log.info("MetaStorage loaded {}", getStorage());
	}

	@SneakyThrows(InterruptedException.class)
	public void loadNamespaces() {


		ExecutorService loaders = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		DatasetRegistry<? extends Namespace> registry = getDatasetRegistry();

		// Namespaces load their storage themselves, so they can inject Namespace relevant objects into stored objects
		StorageHandler storageHandler = registry.getStorageHandler();
		final Collection<NamespaceStorage> namespaceStorages = getConfig().getStorage().discoverNamespaceStorages(storageHandler);
		for (NamespaceStorage namespaceStorage : namespaceStorages) {
			loaders.submit(() -> {
				registry.createNamespace(namespaceStorage);
			});
		}


		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {
			final int coundLoaded = registry.getDatasets().size();
			log.debug("Waiting for Worker namespaces to load. {} are already finished. {} pending.", coundLoaded, namespaceStorages.size()
																												  - coundLoaded);
		}
	}

	@Override
	public void start() throws Exception {
		manager.start();
	}

	@Override
	public void stop() throws Exception {
		manager.stop();
		for (ResourcesProvider provider : providers) {
			try {
				provider.close();
			}
			catch (Exception e) {
				log.error(provider + " could not be closed", e);
			}

		}

		try {
			getStorage().close();
		}
		catch (Exception e) {
			log.error("{} could not be closed", getStorage(), e);
		}

		client.close();

	}
}
