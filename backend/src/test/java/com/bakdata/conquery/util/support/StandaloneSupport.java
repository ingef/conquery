package com.bakdata.conquery.util.support;

import java.io.Closeable;
import java.io.File;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.validation.Validator;
import javax.ws.rs.client.Client;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.network.specific.RemoveWorker;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.google.common.util.concurrent.MoreExecutors;
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
	private final AdminProcessor datasetsProcessor;
	@Getter
	private final User testUser;


	public void waitUntilWorkDone() {
		log.info("Waiting for jobs to finish");
		boolean busy;
		//sample 10 times from the job queues to make sure we are done with everything
		long started = System.nanoTime();
		for(int i=0;i<10;i++) {
			do {
				busy = standaloneCommand.getMaster().getJobManager().isSlowWorkerBusy();
				for (SlaveCommand slave : standaloneCommand.getSlaves()) {
					busy |= slave.getJobManager().isSlowWorkerBusy();

					for (Worker worker : slave.getWorkers().getWorkers().values()) {
						busy |= worker.getJobManager().isSlowWorkerBusy();
					}
				}

				Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS);
				if(Duration.ofNanos(System.nanoTime()-started).toSeconds()>10) {
					log.warn("waiting for done work for a long time");
					started = System.nanoTime();
				}
			} while(busy);
		}
		log.info("all jobs finished");
	}

	public void preprocessTmp() {
		DropwizardTestSupport<ConqueryConfig> prepro = new DropwizardTestSupport<>(
			Conquery.class,
			config,
			app -> new TestCommandWrapper(config, new PreprocessorCommand(MoreExecutors.newDirectExecutorService()))
		);
		prepro.before();
		prepro.after();
	}

	@Override
	public void close() {
		DatasetId dataset = getDataset().getId();
		standaloneCommand.getMaster().getNamespaces().getSlaves().values().forEach(s -> s.send(new RemoveWorker(dataset)));
		standaloneCommand.getMaster().getNamespaces().removeNamespace(dataset);
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
