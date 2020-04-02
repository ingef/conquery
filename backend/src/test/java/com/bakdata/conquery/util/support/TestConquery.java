package com.bakdata.conquery.util.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.Validator;
import javax.ws.rs.client.Client;

import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.messages.network.SlaveMessage;
import com.bakdata.conquery.models.messages.network.specific.RemoveWorker;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.Wait;
import com.bakdata.conquery.util.io.Cloner;
import com.google.common.io.Files;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Represents the test instance of Conquery.
 *
 */
@Slf4j
public class TestConquery implements Extension, BeforeAllCallback, AfterAllCallback, AfterEachCallback {

	private static final ConcurrentHashMap<String, Integer> NAME_COUNTS = new ConcurrentHashMap<>();

	@Getter
	private StandaloneCommand standaloneCommand;
	@Getter
	private DropwizardTestSupport<ConqueryConfig> dropwizard;
	private File tmpDir;
	private ConqueryConfig config;
	private Set<StandaloneSupport> openSupports = new HashSet<>();
	@Getter
	private Client client;

	/**
	 * Returns the extension context used by the beforeAll-callback.
	 *
	 * @return The context.
	 */
	@Getter
	private ExtensionContext beforeAllContext;

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
			DatasetId datasetId = new DatasetId(name);
			standaloneCommand.getMaster().getAdmin().getAdminProcessor().addDataset(name);
			return createSupport(datasetId, name);
		}
		catch (Exception e) {
			return fail("Failed to create a support for " + name, e);
		}
	}

	private synchronized StandaloneSupport createSupport(DatasetId datasetId, String name) {
		Namespaces namespaces = standaloneCommand.getMaster().getNamespaces();
		Namespace ns = namespaces.get(datasetId);

		assertThat(namespaces.getSlaves()).hasSize(2);

		// make tmp subdir and change cfg accordingly
		File localTmpDir = new File(tmpDir, "tmp_" + name);
		localTmpDir.mkdir();
		ConqueryConfig localCfg = Cloner.clone(config, Map.of(Validator.class, standaloneCommand.getMaster().getEnvironment().getValidator()));
		localCfg
			.getPreprocessor()
			.setDirectories(new PreprocessingDirectories[] { new PreprocessingDirectories(localTmpDir, localTmpDir, localTmpDir) });

		StandaloneSupport support = new StandaloneSupport(
			this,
			standaloneCommand,
			ns,
			ns.getStorage().getDataset(),
			localTmpDir,
			localCfg,
			standaloneCommand.getMaster().getAdmin().getAdminProcessor(),
			// Getting the User from AuthorizationConfig
			standaloneCommand.getMaster().getConfig().getAuthorization().getInitialUsers().get(0).getUser());

		Wait.builder().attempts(100).stepTime(50).build().until(() -> ns.getWorkers().size() == ns.getNamespaces().getSlaves().size());

		support.waitUntilWorkDone();
		openSupports.add(support);
		return support;
	}


	public synchronized void shutdown(StandaloneSupport support) {
		log.info("Tearing down dataset");


		DatasetId dataset = support.getDataset().getId();
		standaloneCommand.getMaster().getNamespaces().getSlaves().values().forEach(s -> s.send(new SlaveMessage() {
			@Override
			public void react(NetworkMessageContext.Slave context) throws Exception {
				context.getWorkers().getWorkers().values().stream()
					   .filter(worker -> worker.getInfo().getDataset().equals(dataset))
					   .forEach(worker -> {
						   try {
							   worker.getStorage().close();
						   } catch (IOException e) {
							   log.error("Failed closing down worker", e);
						   }
					   });

			}
		}));
		try {
			standaloneCommand.getMaster().getStorage().close();
		} catch (IOException e) {
			log.error("",e);
		}
	}

	protected ConqueryConfig getConfig() throws Exception {
		ConqueryConfig config = new ConqueryConfig();

		config.getPreprocessor().setDirectories(new PreprocessingDirectories[] { new PreprocessingDirectories(tmpDir, tmpDir, tmpDir) });
		config.getStorage().setDirectory(tmpDir);
		config.getStandalone().setNumberOfSlaves(2);
		// configure logging
		config.setLoggingFactory(new TestLoggingFactory());

		// set random open ports
		for (ConnectorFactory con : CollectionUtils
			.union(
				((DefaultServerFactory) config.getServerFactory()).getAdminConnectors(),
				((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors())) {
			try (ServerSocket s = new ServerSocket(0)) {
				((HttpConnectorFactory) con).setPort(s.getLocalPort());
			}
		}
		try (ServerSocket s = new ServerSocket(0)) {
			config.getCluster().setPort(s.getLocalPort());
		}

		// make buckets very small
		// but not so small that we can't test bucket problems
		config.getCluster().setEntityBucketSize(3);

		return config;
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		this.beforeAllContext = context;
		// create tmp dir if it was not already created
		if (tmpDir == null) {
			tmpDir = Files.createTempDir();
		}
		log.info("Working in temporary directory {}", tmpDir);

		config = getConfig();
		context
			.getTestInstance()
			.filter(ConfigOverride.class::isInstance)
			.map(ConfigOverride.class::cast)
			.ifPresent(co -> co.override(config));

		// define server
		dropwizard = new DropwizardTestSupport<ConqueryConfig>(TestBootstrappingConquery.class, config, app -> {
			standaloneCommand = new StandaloneCommand((TestBootstrappingConquery) app);
			return new TestCommandWrapper(config, standaloneCommand);
		});

		// start server
		dropwizard.before();

		// create HTTP client for api tests
		client = new JerseyClientBuilder(this.getDropwizard().getEnvironment())
			.withProperty(ClientProperties.CONNECT_TIMEOUT, 10000)
			.withProperty(ClientProperties.READ_TIMEOUT, 10000)
			.build("test client");
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		client.close();
		dropwizard.after();
		FileUtils.deleteQuietly(tmpDir);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		for (Iterator<StandaloneSupport> it = openSupports.iterator(); it.hasNext(); ) {
			StandaloneSupport openSupport = it.next();

			log.info("Tearing down dataset");
			DatasetId dataset = openSupport.getDataset().getId();
			standaloneCommand.getMaster().getNamespaces().getSlaves().values().forEach(s -> s.send(new RemoveWorker(dataset)));
			standaloneCommand.getMaster().getNamespaces().removeNamespace(dataset);
			it.remove();
		}
	}
}
