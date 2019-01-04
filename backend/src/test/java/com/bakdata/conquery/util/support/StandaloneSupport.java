package com.bakdata.conquery.util.support;

import java.io.Closeable;
import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.validation.Validator;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.jobs.JobManager;
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
	private final StandaloneCommand standaloneCommand;
	@Getter
	private final Namespace namespace;
	@Getter
	private final Dataset dataset;
	@Getter
	private final File tmpDir;
	@Getter
	private final ConqueryConfig cfg;
	@Getter
	private final DatasetsProcessor datasetsProcessor;


	public void waitUntilWorkDone() {
		log.info("Waiting for jobs to finish");
		waitUntilWorkDone(standaloneCommand.getMaster().getJobManager());
		for (SlaveCommand slave : standaloneCommand.getSlaves())
			waitUntilWorkDone(slave.getJobManager());
		waitUntilWorkDone(standaloneCommand.getMaster().getJobManager());
		log.info("all jobs finished");
	}

	private void waitUntilWorkDone(JobManager jobManager) {
		while (jobManager.isSlowWorkerBusy()) {
			Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
		}
	}
	
	public void preprocessTmp() {
		DropwizardTestSupport<ConqueryConfig> prepro = new DropwizardTestSupport<>(
				Conquery.class,
				cfg,
				app -> new TestCommandWrapper(cfg, new PreprocessorCommand())
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
}
