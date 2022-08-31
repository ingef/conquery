package com.bakdata.conquery.models.jobs;

import java.io.Closeable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobManager implements Closeable{

	private final JobExecutor slowExecutor;
	private final JobExecutor fastExecutor;

	private final Thread.UncaughtExceptionHandler notifyExecutorDied = (thread, ex) -> { System.exit(1);};

	public JobManager(String name, boolean failOnError) {

		slowExecutor = new JobExecutor("Job Manager slow " + name, failOnError);
		fastExecutor = new JobExecutor("Job Manager fast " + name, failOnError);

		slowExecutor.setUncaughtExceptionHandler(notifyExecutorDied);
		fastExecutor.setUncaughtExceptionHandler(notifyExecutorDied);

		slowExecutor.start();
		fastExecutor.start();
	}

	public void addSlowJob(Job job) {
		log.trace("Added job {}", job.getLabel());
		slowExecutor.add(job);
	}
	
	public void addFastJob(Job job) {
		fastExecutor.add(job);
	}
	
	public List<Job> getSlowJobs() {
		return slowExecutor.getJobs();
	}

	public JobManagerStatus reportStatus() {

		return new JobManagerStatus(
				getSlowJobs()
						.stream()
						.map(job -> new JobStatus(job.getJobId(), job.getProgressReporter(), job.getLabel(), job.isCancelled()))
						.collect(Collectors.toList())
		);
	}

	public boolean isSlowWorkerBusy() {
		return slowExecutor.isBusy();
	}

	public boolean cancelJob(UUID jobId) {
		return fastExecutor.cancelJob(jobId) || slowExecutor.cancelJob(jobId);
	}

	@Override
	public void close() {
		fastExecutor.close();
		slowExecutor.close();
	}
}
