package com.bakdata.conquery.util.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.validation.Validator;
import javax.ws.rs.client.Client;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.DistributedStandaloneCommand;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.integration.sql.SqlStandaloneCommand;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.cluster.ClusterManager;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.Wait;
import com.bakdata.conquery.util.io.Cloner;
import com.google.common.util.concurrent.Uninterruptibles;
import io.dropwizard.cli.Command;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.extension.ExtensionContext;

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
	private StandaloneCommand standaloneCommand;
	@Getter
	private DropwizardTestSupport<ConqueryConfig> dropwizard;
	private Set<StandaloneSupport> openSupports = new HashSet<>();
	@Getter
	private Client client;

	private AtomicBoolean started = new AtomicBoolean(false);

	/**
	 * Returns the extension context used by the beforeAll-callback.
	 *
	 * @return The context.
	 */
	@Getter
	private ExtensionContext beforeAllContext;
	// Initial user which is set before each test from the config.
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

	public synchronized StandaloneSupport getSupport(String name) {
		try {
			log.info("Setting up dataset");
			int count = NAME_COUNTS.merge(name, 0, (a, b) -> a + 1);
			if (count > 0) {
				name += "[" + count + "]";
			}
			Dataset dataset = new Dataset(name);
			standaloneCommand.getManagerNode().getAdmin().getAdminDatasetProcessor().addDataset(dataset);
			return createSupport(dataset.getId(), name);
		}
		catch (Exception e) {
			return fail("Failed to create a support for " + name, e);
		}
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
		dropwizard = new DropwizardTestSupport<ConqueryConfig>(TestBootstrappingConquery.class, config, app -> {
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
	}

	public void afterAll() throws Exception {
		client.close();
		dropwizard.after();
		FileUtils.deleteQuietly(tmpDir);
	}

	public void afterEach() throws Exception {
		synchronized (openSupports) {
			for (StandaloneSupport openSupport : openSupports) {
				removeSupportDataset(openSupport);
			}
			openSupports.clear();
		}
		this.getStandaloneCommand().getManagerNode().getStorage().clear();
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
					log.warn("waiting for done work for a long time", new Exception());
				}

			} while (true);
		}
		log.trace("all jobs finished");
	}

	public void beforeEach() {
		final MetaStorage storage = standaloneCommand.getManagerNode().getStorage();
		testUser = standaloneCommand.getManagerNode().getConfig().getAuthorizationRealms().getInitialUsers().get(0).createOrOverwriteUser(storage);
		storage.updateUser(testUser);
	}

	private synchronized StandaloneSupport createSupport(DatasetId datasetId, String name) {
		if (config.getSqlConnectorConfig().isEnabled()) {
			return buildSupport(datasetId, name);
		}
		return buildDistributedSupport(datasetId, name);
	}

	private synchronized StandaloneSupport buildDistributedSupport(DatasetId datasetId, String name) {

		ClusterManager manager = (ClusterManager) standaloneCommand.getManager();
		ClusterState clusterState = manager.getConnectionManager().getClusterState();
		assertThat(clusterState.getShardNodes()).hasSize(2);

		Wait.builder()
			.total(Duration.ofSeconds(5))
			.stepTime(Duration.ofMillis(5))
			.build()
			.until(() -> clusterState.getWorkerHandlers().get(datasetId).getWorkers().size() == clusterState.getShardNodes().size());

		return buildSupport(datasetId, name);
	}

	private StandaloneSupport buildSupport(DatasetId datasetId, String name) {

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
				this,
				ns,
				ns.getStorage().getDataset(),
				localTmpDir,
				localCfg,
				standaloneCommand.getManagerNode().getAdmin().getAdminProcessor(),
				standaloneCommand.getManagerNode().getAdmin().getAdminDatasetProcessor(),
				// Getting the User from AuthorizationConfig
				testUser
		);

		support.waitUntilWorkDone();
		openSupports.add(support);
		return support;
	}

	private boolean isBusy() {
		boolean busy;
		busy = standaloneCommand.getManagerNode().getJobManager().isSlowWorkerBusy();
		busy |= standaloneCommand.getManagerNode()
								 .getStorage()
								 .getAllExecutions()
								 .stream()
								 .map(ManagedExecution::getState)
								 .anyMatch(ExecutionState.RUNNING::equals);

		for (Namespace namespace : standaloneCommand.getManagerNode().getDatasetRegistry().getDatasets()) {
			busy |= namespace.getJobManager().isSlowWorkerBusy();
		}

		for (ShardNode shard : standaloneCommand.getShardNodes()) {
			busy |= shard.isBusy();
		}
		return busy;
	}
}
