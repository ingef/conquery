package com.bakdata.conquery.util.support;

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
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.google.common.io.Files;
import com.google.common.util.concurrent.Uninterruptibles;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestConquery implements Extension, BeforeAllCallback, AfterAllCallback {

	private StandaloneCommand standaloneCommand;
	private DropwizardTestSupport<ConqueryConfig> dropwizard;
	private File tmpDir;
	private ConqueryConfig cfg;
	private Set<StandaloneSupport> openSupports = new HashSet<>();
	
	public synchronized StandaloneSupport getSupport() {
		try {
			log.info("Setting up dataset");
	
			String name = UUID.randomUUID().toString();
			DatasetId id = new DatasetId(name);
		
			standaloneCommand.getMaster().getAdmin().getDatasetsProcessor().addDataset(name, standaloneCommand.getMaster().getMaintenanceService());
			Namespace ns = standaloneCommand.getMaster().getNamespaces().get(id);
			
			Dataset dataset = ns.getStorage().getDataset();
			
			for(SlaveInformation slave : standaloneCommand.getMaster().getNamespaces().getSlaves().values()) {
				standaloneCommand.getMaster().getAdmin().getDatasetsProcessor().addWorker(slave, dataset);
			}
			
			StandaloneSupport support = new StandaloneSupport(
				this,
				standaloneCommand,
				ns,
				ns.getStorage().getDataset(),
				tmpDir,
				cfg,
				standaloneCommand.getMaster().getAdmin().getDatasetsProcessor()
			);
			while(ns.getWorkers().size() < standaloneCommand.getMaster().getNamespaces().getSlaves().size()) {
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
		//create config and tmp dir
		tmpDir = Files.createTempDir();
		cfg = new ConqueryConfig();

		cfg.getPreprocessor().setDirectories(
				new PreprocessingDirectories[]{
						new PreprocessingDirectories(tmpDir, tmpDir, tmpDir)
				}
		);
		cfg.getStorage().setDirectory(tmpDir);
		cfg.getStorage().setPreprocessedRoot(tmpDir);
		
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
		FileUtils.deleteDirectory(tmpDir);
	}

	
}
