package com.bakdata.conquery.util.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.io.ConfigCloner;
import com.google.common.io.Files;
import com.google.common.util.concurrent.Uninterruptibles;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents the test instance of Conquery.
 *
 */
@Slf4j
public class TestConquery implements Extension, BeforeAllCallback, AfterAllCallback {

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
		catch(Exception e) {
			return fail(e);
		}
	}
	
	public synchronized StandaloneSupport getSupport() {
		try {
			log.info("Setting up dataset");
			String name = UUID.randomUUID().toString();
			DatasetId datasetId = new DatasetId(name);
			standaloneCommand.getMaster().getAdmin().getDatasetsProcessor().addDataset(name);
			return createSupport(datasetId, name);
		}
		catch(Exception e) {
			return fail(e);
		}
	}
	
	private synchronized StandaloneSupport createSupport(DatasetId datasetId, String name) {
		Namespaces namespaces = standaloneCommand.getMaster().getNamespaces();
		Namespace ns = namespaces.get(datasetId);
		
		assertThat(namespaces.getSlaves()).hasSize(2);
		
		
		//make tmp subdir and change cfg accordingly
		File localTmpDir = new File(tmpDir, "tmp_"+name);
		localTmpDir.mkdir();
		ConqueryConfig localCfg = ConfigCloner.clone(config);
		localCfg.getPreprocessor().setDirectories(
			new PreprocessingDirectories[]{
				new PreprocessingDirectories(localTmpDir, localTmpDir, localTmpDir)
			}
		);
		
		StandaloneSupport support = new StandaloneSupport(
			this,
			standaloneCommand,
			ns,
			ns.getStorage().getDataset(),
			localTmpDir,
			localCfg,
			standaloneCommand.getMaster().getAdmin().getDatasetsProcessor()
		);
		while(ns.getWorkers().size() < ns.getNamespaces().getSlaves().size()) {
			Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
		}
		support.waitUntilWorkDone();
		openSupports.add(support);
		return support;
	}
	
	/*package*/ synchronized void stop(StandaloneSupport support) {
		log.info("Tearing down dataset");
		
		//standaloneCommand.getMaster().getStorage().removeDataset(support.getDataset().getId());
		//standaloneCommand.getMaster().getStorage().getInformation().sendToAll(new RemoveDataset(dataset.getId()));

		openSupports.remove(support);
	}
	
	protected ConqueryConfig getConfig() throws Exception {
		ConqueryConfig config = new ConqueryConfig();

		config.getPreprocessor().setDirectories(
				new PreprocessingDirectories[]{
					new PreprocessingDirectories(tmpDir, tmpDir, tmpDir)
				}
		);
		config.getStorage().setDirectory(tmpDir);
		config.getStorage().setPreprocessedRoot(tmpDir);
		config.getStandalone().setNumberOfSlaves(2);
		//configure logging
		config.setLoggingFactory(new TestLoggingFactory());
		
		//set random open ports
		for(ConnectorFactory con : CollectionUtils.union(
				((DefaultServerFactory)config.getServerFactory()).getAdminConnectors(),
				((DefaultServerFactory)config.getServerFactory()).getApplicationConnectors()
			)
		) {
			try(ServerSocket s = new ServerSocket(0)) {
				((HttpConnectorFactory)con).setPort(s.getLocalPort());
			}
		}
		try(ServerSocket s = new ServerSocket(0)) {
			config.getCluster().setPort(s.getLocalPort());
		}
		
		//make buckets very small
		config.getCluster().setEntityBucketSize(1);

		return config;
	}
	
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		this.beforeAllContext = context;
		//create tmp dir if it was not already created
		if(tmpDir == null) {
			tmpDir = Files.createTempDir();
		}
		log.info("Working in temporary directory {}", tmpDir);
		
		config = getConfig();
		context.getTestInstance()
			.filter(ConfigOverride.class::isInstance)
			.map(ConfigOverride.class::cast)
			.ifPresent(co -> co.override(config));

		//define server
		dropwizard = new DropwizardTestSupport<ConqueryConfig>(
			TestBootstrappingConquery.class,
			config,
			app -> {
				standaloneCommand = new StandaloneCommand((TestBootstrappingConquery) app);
				return new TestCommandWrapper(config, standaloneCommand);
			}
		);

		//start server
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

	
}
