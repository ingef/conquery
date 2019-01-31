package com.bakdata.conquery.util.support;

import java.io.Closeable;
import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.validation.Validator;
import javax.ws.rs.client.Client;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.DatasetsProcessor;
import com.google.common.util.concurrent.Uninterruptibles;

import io.dropwizard.testing.DropwizardTestSupport;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class StandaloneSupport implements Closeable {

	private final TestConquery testConquery;
	@Getter
	private final StandaloneCommand standaloneCommand;
	@Getter
	private final Namespace namespace;
	@Getter
	private final Dataset dataset;
	@Getter
	private final File tmpDir;
	@Getter
	private final ConqueryConfig config;
	@Getter
	private final DatasetsProcessor datasetsProcessor;


	public void waitUntilWorkDone() {
		log.info("Waiting for jobs to finish");
		boolean busy;
		//sample 10 times from the job queues to make sure we are done with everything
		for(int i=0;i<10;i++) {
			do {
				busy = false;
				busy |= standaloneCommand.getMaster().getJobManager().isSlowWorkerBusy();
				for (SlaveCommand slave : standaloneCommand.getSlaves())
					busy |= slave.getJobManager().isSlowWorkerBusy();
				Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
			} while(busy);
		}
		log.info("all jobs finished");
	}

	public void preprocessTmp() {
		DropwizardTestSupport<ConqueryConfig> prepro = new DropwizardTestSupport<>(
			Conquery.class,
			config,
			app -> new TestCommandWrapper(config, new PreprocessorCommand())
		);
		prepro.before();
		prepro.after();
	}

	@Override
	public void close() {
		testConquery.stop(this);
	}

	public Validator getValidator() {
		return standaloneCommand.getMaster().getValidator();
	}
	
	/**
	 * Retrieves the port of the admin API.
	 * @return The port.
	 */
	public int getAdminPort() {
		return testConquery.getDropwizard().getAdminPort();
	}

	public Client getClient() {
		return testConquery.getClient();
	}

	/**
	 * Retrieves the port of the main API.
	 * @return The port.
	 */
	public int getLocalPort() {
		return testConquery.getDropwizard().getLocalPort();
	}
}
