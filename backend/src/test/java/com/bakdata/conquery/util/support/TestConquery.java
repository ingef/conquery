package com.bakdata.conquery.util.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.bakdata.conquery.util.io.ConfigCloner;
import com.google.common.io.Files;
import com.google.common.util.concurrent.Uninterruptibles;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestConquery implements Extension, BeforeAllCallback, AfterAllCallback {

	private StandaloneCommand standaloneCommand;
	@Getter
	private DropwizardTestSupport<ConqueryConfig> dropwizard;
	private File tmpDir;
	private ConqueryConfig cfg;
	private Set<StandaloneSupport> openSupports = new HashSet<>();
	
	public synchronized StandaloneSupport openDataset(DatasetId datasetId) {
		try {
			log.info("loading dataset");
			String name = datasetId.getName();
		
			Namespaces namespaces = standaloneCommand.getMaster().getNamespaces();
			Namespace ns = namespaces.get(datasetId);
			
			Dataset dataset = ns.getStorage().getDataset();
			
			assertThat(namespaces.getSlaves()).hasSize(2);
			
			//make tmp subdir and change cfg accordingly
			File localTmpDir = new File(tmpDir, "tmp_"+name);
			localTmpDir.mkdir();
			ConqueryConfig localCfg = ConfigCloner.clone(cfg);
			localCfg.getPreprocessor().setDirectories(
				new PreprocessingDirectories[]{
					new PreprocessingDirectories(localTmpDir, localTmpDir, tmpDir)
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
			while(ns.getWorkers().size() < namespaces.getSlaves().size()) {
				Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
			}
			support.waitUntilWorkDone();
			openSupports.add(support);
			return support;
		} catch(Exception e) {
			return fail(e);
		}
	}
	
	public synchronized StandaloneSupport getSupport() {
		try {
			log.info("Setting up dataset");
			String name = UUID.randomUUID().toString();
			DatasetId id = new DatasetId(name);
		
			standaloneCommand.getMaster().getAdmin().getDatasetsProcessor().addDataset(name, standaloneCommand.getMaster().getMaintenanceService());
			Namespaces namespaces = standaloneCommand.getMaster().getNamespaces();
			Namespace ns = namespaces.get(id);
			
			Dataset dataset = ns.getStorage().getDataset();
			
			assertThat(namespaces.getSlaves()).hasSize(2);
			for(SlaveInformation slave : namespaces.getSlaves().values()) {
				standaloneCommand.getMaster().getAdmin().getDatasetsProcessor().addWorker(slave, dataset);
			}
			
			//make tmp subdir and change cfg accordingly
			File localTmpDir = new File(tmpDir, "tmp_"+name);
			localTmpDir.mkdir();
			ConqueryConfig localCfg = ConfigCloner.clone(cfg);
			localCfg.getPreprocessor().setDirectories(
				new PreprocessingDirectories[]{
					new PreprocessingDirectories(localTmpDir, localTmpDir, tmpDir)
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
			while(ns.getWorkers().size() < namespaces.getSlaves().size()) {
				Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
			}
			support.waitUntilWorkDone();
			openSupports.add(support);
			return support;
		} catch(Exception e) {
			return fail(e);
		}
	}
	
	/*package*/ synchronized void stop(StandaloneSupport support) {
		log.info("Tearing down dataset");
		
		//standaloneCommand.getMaster().getStorage().removeDataset(support.getDataset().getId());
		//standaloneCommand.getMaster().getStorage().getInformation().sendToAll(new RemoveDataset(dataset.getId()));

		openSupports.remove(support);
	}
	
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		//create tmp dir if it was not already created
		if(tmpDir == null) {
			tmpDir = Files.createTempDir();
		}
		log.info("Working in temporary directory {}", tmpDir);
		cfg = new ConqueryConfig();

		cfg.getPreprocessor().setDirectories(
				new PreprocessingDirectories[]{
					new PreprocessingDirectories(tmpDir, tmpDir, tmpDir)
				}
		);
		cfg.getStorage().setDirectory(tmpDir);
		cfg.getStorage().setPreprocessedRoot(tmpDir);
		cfg.getStandalone().setNumberOfSlaves(2);
		
		//set random open ports
		for(ConnectorFactory con : CollectionUtils.union(
				((DefaultServerFactory)cfg.getServerFactory()).getAdminConnectors(),
				((DefaultServerFactory)cfg.getServerFactory()).getApplicationConnectors()
			)
		) {
			try(ServerSocket s = new ServerSocket(0)) {
				((HttpConnectorFactory)con).setPort(s.getLocalPort());
			}
		}
		try(ServerSocket s = new ServerSocket(0)) {
			cfg.getCluster().setPort(s.getLocalPort());
		}
		
		//make buckets very small
		cfg.getCluster().setEntityBucketSize(1);

		//define server
		dropwizard = new DropwizardTestSupport<ConqueryConfig>(
				Conquery.class,
				cfg,
				app -> {
					standaloneCommand = new StandaloneCommand((Conquery) app);
					return new TestCommandWrapper(cfg, standaloneCommand);
				}
		);
		
		//start server
		dropwizard.before();
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		dropwizard.after();
		FileUtils.deleteQuietly(tmpDir);
	}

	
}
