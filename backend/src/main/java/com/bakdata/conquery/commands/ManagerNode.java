package com.bakdata.conquery.commands;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.validation.Validator;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.Manager;
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
import com.bakdata.conquery.tasks.LoadStorageTask;
import com.bakdata.conquery.tasks.PermissionCleanupTask;
import com.bakdata.conquery.tasks.QueryCleanupTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Central node of Conquery. Hosts the frontend, api, metadata and takes care of query distribution to
 * {@link ShardNode}s and respectively the {@link Worker}s hosted on them. The {@link ManagerNode} can also
 * forward queries or results to statistic backends. Finally, it collects the results of queries for access over the api.
 */
@Slf4j
@Getter
public class ManagerNode implements Managed {

	public static final String DEFAULT_NAME = "manager";

	private final String name;
	private final List<ResourcesProvider> providers = new ArrayList<>();
	private Validator validator;
	private AdminServlet admin;
	private AuthorizationController authController;
	private ScheduledExecutorService maintenanceService;
	@Delegate(excludes = Managed.class)
	private Manager manager;

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

		this.manager = manager;

		final ObjectMapper apiObjectMapper = environment.getObjectMapper();
		getInternalMapperFactory().customizeApiObjectMapper(apiObjectMapper, getDatasetRegistry(), getMetaStorage());


		// FormScanner needs to be instantiated before plugins are initialized
		formScanner = new FormScanner(config);


		// Init all plugins
		config.getPlugins().forEach(pluginConfig -> pluginConfig.initialize(this));


		// Initialization of internationalization
		I18n.init();

		configureApiServlet(config, environment);

		maintenanceService = environment.lifecycle()
										.scheduledExecutorService("Maintenance Service")
										.build();

		environment.lifecycle().manage(this);

		loadNamespaces();

		loadMetaStorage();

		// Create AdminServlet first to make it available to the realms
		admin = new AdminServlet(this);

		authController = new AuthorizationController(getMetaStorage(), config, environment, admin);
		environment.lifecycle().manage(authController);

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

		formScanner.execute(null, null);

		registerTasks(manager, environment, config);
	}

	private void configureApiServlet(ConqueryConfig config, Environment environment) {
		ResourceConfig jerseyConfig = environment.jersey().getResourceConfig();
		RESTServer.configure(config, jerseyConfig);
		jerseyConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(getMetaStorage()).to(MetaStorage.class);
				bind(getDatasetRegistry()).to(DatasetRegistry.class);
			}
		});

		getInternalMapperFactory().customizeApiObjectMapper(environment.getObjectMapper(), getDatasetRegistry(), getMetaStorage());

	}

	@SneakyThrows(InterruptedException.class)
	public void loadNamespaces() {

		try(ExecutorService loaders = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
			DatasetRegistry<? extends Namespace> registry = getDatasetRegistry();

			// Namespaces load their storage themselves, so they can inject Namespace relevant objects into stored objects
			final Collection<NamespaceStorage> namespaceStorages = getConfig().getStorage().discoverNamespaceStorages();
			for (NamespaceStorage namespaceStorage : namespaceStorages) {
				loaders.submit(() -> {
					registry.createNamespace(namespaceStorage, getMetaStorage(), getEnvironment());
				});
			}


			loaders.shutdown();
			while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {
				final int countLoaded = registry.getNamespaces().size();
				log.debug("Waiting for Worker namespaces to load. {} are already finished. {} pending.", countLoaded, namespaceStorages.size()
																													  - countLoaded);
			}
		}
	}

	private void loadMetaStorage() {
		log.info("Opening MetaStorage");
		getMetaStorage().openStores(getInternalMapperFactory().createManagerPersistenceMapper(getDatasetRegistry(), getMetaStorage()));

		getMetaStorage().loadKeys();

		if (getConfig().getStorage().isLoadStoresOnStart()) {
			log.info("BEGIN loading MetaStorage");
			getMetaStorage().loadData();
			log.debug("DONE loading MetaStorage {}", getMetaStorage());
		}
	}

	private void registerTasks(Manager manager, Environment environment, ConqueryConfig config) {
		environment.admin().addTask(formScanner);
		environment.admin().addTask(
				new QueryCleanupTask(getMetaStorage(), Duration.of(
						config.getQueries().getOldQueriesTime().getQuantity(),
						config.getQueries().getOldQueriesTime().getUnit().toChronoUnit()
				)));

		environment.admin().addTask(new PermissionCleanupTask(getMetaStorage()));
		manager.getAdminTasks().forEach(environment.admin()::addTask);
		environment.admin().addTask(new LoadStorageTask(getName(), getMetaStorage(), getDatasetRegistry()));

		final ShutdownTask shutdown = new ShutdownTask();
		environment.admin().addTask(shutdown);
		environment.lifecycle().addServerLifecycleListener(shutdown);
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
				log.error("{} could not be closed", provider, e);
			}

		}

		try {
			getMetaStorage().close();
		}
		catch (Exception e) {
			log.error("{} could not be closed", getMetaStorage(), e);
		}

	}
}
