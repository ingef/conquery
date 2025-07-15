package com.bakdata.conquery.util.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import jakarta.validation.Validator;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.UriBuilder;

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
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.util.io.Cloner;
import com.google.common.base.Stopwatch;
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
	@Getter
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
		String safeFileName = name.replaceAll("\\W", "");
		File localTmpDir = new File(tmpDir, "tmp_" + safeFileName);

		if (!localTmpDir.exists()) {
			if (!localTmpDir.mkdir()) {
				throw new IllegalStateException("Could not create directory for Support:" + localTmpDir);
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
				ns.getStorage().getDataset().getId(),
				localTmpDir,
				localCfg,
				// Getting the User from AuthorizationConfig
				testDataImporter
		);

		openSupports.add(support);
		return support;
	}

	private synchronized StandaloneSupport buildDistributedSupport(DatasetId datasetId, String name) {

		ClusterManager manager = (ClusterManager) standaloneCommand.getManager();
		ClusterState clusterState = manager.getConnectionManager().getClusterState();
		assertThat(clusterState.getShardNodes()).hasSize(2);

		waitUntil(() -> clusterState.getWorkerHandlers().get(datasetId).getWorkers().size() == clusterState.getShardNodes().size());

		return buildSupport(datasetId, name, StandaloneSupport.Mode.WORKER);
	}

	@SneakyThrows
	public static void waitUntil(Supplier<Boolean> condition) {
		Stopwatch stopwatch = Stopwatch.createStarted();
		int done = 0;

		while (stopwatch.elapsed(TimeUnit.SECONDS) < 10) {
			Thread.sleep(2);
			if (!condition.get()) {
				continue;
			}

			//sample multiple times from the job queues to make sure we are done with everything and don't miss late arrivals
			done++;
			if (done > 5) {
				return;
			}
		}

		throw new IllegalStateException("Jobs did not finish within expected time.");
	}

	public synchronized StandaloneSupport getSupport(String name) {
		try {
			log.info("Setting up dataset");
			int count = NAME_COUNTS.merge(name, 0, (a, b) -> a + 1);
			if (count > 0) {
				name += "[" + count + "]";
			}
			Dataset dataset = new Dataset(name);
			dataset.setStorageProvider(getDatasetRegistry());

			LoadingUtil.importDataset(getClient(), defaultAdminURIBuilder(), dataset);
			waitUntilWorkDone();

			// Little detour here, but this way we get the correctly initialized dataset id
			DatasetId datasetId = getDatasetRegistry().get(dataset.getId()).getDataset().getId();

			return createSupport(datasetId, name);
		}
		catch (Exception e) {
			return fail("Failed to create a support for " + name, e);
		}
	}

	public DatasetRegistry<?> getDatasetRegistry() {
		return getStandaloneCommand().getManagerNode().getDatasetRegistry();
	}

	public UriBuilder defaultAdminURIBuilder() {
		return UriBuilder.fromPath("admin")
						 .host("localhost")
						 .scheme("http")
						 .port(dropwizard.getAdminPort());
	}

	@SneakyThrows
	public void waitUntilWorkDone() {
		log.trace("Waiting for jobs to finish");
		waitUntil(() -> !isBusy());
	}

	private boolean isBusy() {
		boolean busy;
		busy = standaloneCommand.getManagerNode().getJobManager().isSlowWorkerBusy();

		busy |= standaloneCommand.getManager().getDatasetRegistry().getNamespaces().stream()
								 .map(Namespace::getExecutionManager)
								 .flatMap(e -> e.getExecutionInfos().asMap().values().stream())
								 .map(ExecutionManager.ExecutionInfo::getExecutionState)
								 .anyMatch(ExecutionState.RUNNING::equals);

		for (Namespace namespace : standaloneCommand.getManagerNode().getDatasetRegistry().getNamespaces()) {
			busy |= namespace.getJobManager().isSlowWorkerBusy();

			if (namespace instanceof DistributedNamespace distributedNamespace) {
				busy |= distributedNamespace.getWorkerHandler().hasPendingMessages();
			}
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
				standaloneCommand = new SqlStandaloneCommand();
			}
			else {
				standaloneCommand = new DistributedStandaloneCommand();
			}
			return (Command) standaloneCommand;
		}
		);
		// start server
		dropwizard.before();

		// create HTTP client for api tests
		client = new JerseyClientBuilder(getDropwizard().getEnvironment())
				.withProperty(ClientProperties.CONNECT_TIMEOUT, 10000)
				.withProperty(ClientProperties.READ_TIMEOUT, 10000)
				.build("test client");


		// The test user is recreated after each test, in the storage, but its id stays the same.
		// Here we register the client filter once for that test user id.
		UserId testUserId = config.getAuthorizationRealms().getInitialUsers().getFirst().createId();
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
		getStandaloneCommand().getManagerNode().getMetaStorage().clear();
		waitUntilWorkDone();
	}

	@SneakyThrows
	public void removeSupportDataset(StandaloneSupport support) {
		standaloneCommand.getManagerNode().getDatasetRegistry().removeNamespace(support.getDataset());
	}

	public void removeSupport(StandaloneSupport support) {
		synchronized (openSupports) {
			openSupports.remove(support);
			removeSupportDataset(support);
		}
	}

	public void beforeEach() {

		// Because Shiro works with a static Security manager
		getStandaloneCommand().getManagerNode().getAuthController().registerStaticSecurityManager();

		// MetaStorage is cleared after each test, so we need to add the test user again
		final MetaStorage storage = standaloneCommand.getManagerNode().getMetaStorage();
		testUser = standaloneCommand.getManagerNode().getConfig().getAuthorizationRealms().getInitialUsers().getFirst().createOrOverwriteUser(storage);
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
