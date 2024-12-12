package com.bakdata.conquery.util.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import jakarta.validation.Validator;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.DistributedStandaloneCommand;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.TestDataImporter;
import com.bakdata.conquery.integration.sql.SqlStandaloneCommand;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.cluster.ClusterManager;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.util.io.Cloner;
import com.google.common.util.concurrent.Uninterruptibles;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.core.cli.Command;
import io.dropwizard.testing.DropwizardTestSupport;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.client.ClientProperties;

/**
 * Represents the test instance of Conquery.
 */
@Slf4j
@RequiredArgsConstructor
public class TestConquery {

	private static final ConcurrentHashMap<String, Integer> NAME_COUNTS = new ConcurrentHashMap<>();
	private final File tmpDir;
	private final ConqueryConfig config;
	private final TestDataImporter testDataImporter;
	private final Set<StandaloneSupport> openSupports = new HashSet<>();
	@Getter
	private StandaloneCommand standaloneCommand;
	@Getter
	private DropwizardTestSupport<ConqueryConfig> dropwizard;
	@Getter
	private Client client;

	// Initial user which is set before each test from the config.
	@Getter
	private User testUser;

	public synchronized StandaloneSupport openDataset(DatasetId datasetId) {
		try {
			log.info("loading dataset");
			return createSupport(datasetId, datasetId.getName());
		}
		catch (Exception e) {
			return fail("Failed to open dataset " + datasetId, e);
		}
	}

	private synchronized StandaloneSupport createSupport(DatasetId datasetId, String name) {
		if (config.getSqlConnectorConfig().isEnabled()) {
			return buildSupport(datasetId, name, StandaloneSupport.Mode.SQL);
		}
		return buildDistributedSupport(datasetId, name);
	}

	private StandaloneSupport buildSupport(DatasetId datasetId, String name, StandaloneSupport.Mode mode) {

		DatasetRegistry<? extends Namespace> datasets = standaloneCommand.getManager().getDatasetRegistry();
		Namespace ns = datasets.get(datasetId);

		// make tmp subdir and change cfg accordingly
		File localTmpDir = new File(tmpDir, "tmp_" + name);

		if (!localTmpDir.exists()) {
			if (!localTmpDir.mkdir()) {
				throw new IllegalStateException("Could not create directory for Support");
			}
		}
		else {
			log.info("Reusing existing folder {} for Support", localTmpDir.getPath());
		}

		ConqueryConfig
				localCfg =
				Cloner.clone(config, Map.of(Validator.class, standaloneCommand.getManagerNode().getEnvironment().getValidator()), IntegrationTests.MAPPER);

		StandaloneSupport support = new StandaloneSupport(
				mode,
				this,
				ns,
				ns.getStorage().getDataset(),
				localTmpDir,
				localCfg,
				// Getting the User from AuthorizationConfig
				testDataImporter
		);

		support.waitUntilWorkDone();
		openSupports.add(support);
		return support;
	}

	private synchronized StandaloneSupport buildDistributedSupport(DatasetId datasetId, String name) {

		ClusterManager manager = (ClusterManager) standaloneCommand.getManager();
		ClusterState clusterState = manager.getConnectionManager().getClusterState();
		assertThat(clusterState.getShardNodes()).hasSize(2);

		await().atMost(10, TimeUnit.SECONDS)
			   .until(() -> clusterState.getWorkerHandlers().get(datasetId).getWorkers().size() == clusterState.getShardNodes().size());

		return buildSupport(datasetId, name, StandaloneSupport.Mode.WORKER);
	}

	public synchronized StandaloneSupport getSupport(String name) {
		try {
			log.info("Setting up dataset");
			int count = NAME_COUNTS.merge(name, 0, (a, b) -> a + 1);
			if (count > 0) {
				name += "[" + count + "]";
			}
			Dataset dataset = new Dataset(name);
			waitUntilWorkDone();
			LoadingUtil.importDataset(getClient(), defaultAdminURIBuilder(), dataset);

			// Little detour here, but this way we get the correctly initialized dataset id
			DatasetId datasetId = getDatasetRegistry().get(new DatasetId(dataset.getName())).getDataset().getId();
			waitUntilWorkDone();

			return createSupport(datasetId, name);
		}
		catch (Exception e) {
			return fail("Failed to create a support for " + name, e);
		}
	}

	public void waitUntilWorkDone() {
		log.info("Waiting for jobs to finish");
		//sample multiple times from the job queues to make sure we are done with everything and don't miss late arrivals
		long started = System.nanoTime();
		for (int i = 0; i < 5; i++) {
			do {
				Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);

				if (!isBusy()) {
					break;
				}


				if (Duration.ofNanos(System.nanoTime() - started).toSeconds() > 10) {
					started = System.nanoTime();
					log.warn("Waiting for done work for a long time", new Exception("This Exception marks the stacktrace, to show where we are waiting."));
				}

			} while (true);
		}
		log.trace("All jobs finished");
	}

	public UriBuilder defaultAdminURIBuilder() {
		return UriBuilder.fromPath("admin")
						 .host("localhost")
						 .scheme("http")
						 .port(dropwizard.getAdminPort());
	}

	public DatasetRegistry<?> getDatasetRegistry() {
		return getStandaloneCommand().getManagerNode().getDatasetRegistry();
	}

	private boolean isBusy() {
		boolean busy;
		busy = standaloneCommand.getManagerNode().getJobManager().isSlowWorkerBusy();
		busy |= standaloneCommand.getManager().getDatasetRegistry().getDatasets().stream()
								 .map(Namespace::getExecutionManager)
								 .flatMap(e -> e.getExecutionStates().asMap().values().stream())
								 .map(ExecutionManager.State::getState)
								 .anyMatch(ExecutionState.RUNNING::equals);

		for (Namespace namespace : standaloneCommand.getManagerNode().getDatasetRegistry().getDatasets()) {
			busy |= namespace.getJobManager().isSlowWorkerBusy();
		}

		for (ShardNode shard : standaloneCommand.getShardNodes()) {
			busy |= shard.isBusy();
		}
		return busy;
	}

	@SneakyThrows
	public synchronized void shutdown() {
		//stop dropwizard directly so ConquerySupport does not delete the tmp directory
		getDropwizard().after();
		openSupports.clear();
	}

	public void beforeAll() throws Exception {

		log.info("Working in temporary directory {}", tmpDir);


		// define server
		dropwizard = new DropwizardTestSupport<>(TestBootstrappingConquery.class, config, app -> {
			if (config.getSqlConnectorConfig().isEnabled()) {
				standaloneCommand = new SqlStandaloneCommand((Conquery) app);
			}
			else {
				standaloneCommand = new DistributedStandaloneCommand((Conquery) app);
			}
			return (Command) standaloneCommand;
		});
		// start server
		dropwizard.before();

		// create HTTP client for api tests
		client = new JerseyClientBuilder(this.getDropwizard().getEnvironment())
				.withProperty(ClientProperties.CONNECT_TIMEOUT, 10000)
				.withProperty(ClientProperties.READ_TIMEOUT, 10000)
				.build("test client");



		// The test user is recreated after each test, in the storage, but its id stays the same.
		// Here we register the client filter once for that test user id.
		UserId testUserId = config.getAuthorizationRealms().getInitialUsers().get(0).createId();
		client.register(new ConqueryAuthenticationFilter(() -> getAuthorizationController().getConqueryTokenRealm().createTokenForUser(testUserId)));

		testUser = getMetaStorage().getUser(testUserId);
	}

	public AuthorizationController getAuthorizationController() {
		return getStandaloneCommand().getManagerNode().getAuthController();
	}

	public MetaStorage getMetaStorage() {
		return getStandaloneCommand().getManagerNode().getMetaStorage();
	}

	public void afterAll() {
		client.close();
		dropwizard.after();
		FileUtils.deleteQuietly(tmpDir);
	}

	public void afterEach() {
		synchronized (openSupports) {
			for (StandaloneSupport openSupport : openSupports) {
				removeSupportDataset(openSupport);
			}
			openSupports.clear();
		}
		this.getStandaloneCommand().getManagerNode().getMetaStorage().clear();
		waitUntilWorkDone();
	}

	@SneakyThrows
	public void removeSupportDataset(StandaloneSupport support) {
		standaloneCommand.getManagerNode().getDatasetRegistry().removeNamespace(support.getDataset().getId());
	}

	public void removeSupport(StandaloneSupport support) {
		synchronized (openSupports) {
			openSupports.remove(support);
			removeSupportDataset(support);
			waitUntilWorkDone();
		}
	}

	public void beforeEach() {

		// Because Shiro works with a static Security manager
		getStandaloneCommand().getManagerNode().getAuthController().registerStaticSecurityManager();

		// MetaStorage is cleared after each test, so we need to add the test user again
		final MetaStorage storage = standaloneCommand.getManagerNode().getMetaStorage();
		testUser = standaloneCommand.getManagerNode().getConfig().getAuthorizationRealms().getInitialUsers().get(0).createOrOverwriteUser(storage);
	}

	public Validator getValidator() {
		return getStandaloneCommand().getManagerNode().getValidator();
	}

	public List<ShardNode> getShardNodes() {
		return getStandaloneCommand().getShardNodes();
	}

	public AdminProcessor getAdminProcessor() {
		return standaloneCommand.getManagerNode().getAdmin().getAdminProcessor();
	}

	public AdminDatasetProcessor getAdminDatasetsProcessor() {
		return standaloneCommand.getManagerNode().getAdmin().getAdminDatasetProcessor();
	}

	public UriBuilder defaultApiURIBuilder() {
		return UriBuilder.fromPath("api")
						 .host("localhost")
						 .scheme("http")
						 .port(dropwizard.getLocalPort());
	}
}
