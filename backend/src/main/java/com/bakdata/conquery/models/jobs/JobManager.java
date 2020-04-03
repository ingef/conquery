package com.bakdata.conquery.models.jobs;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobManager implements Managed {
	private final JobExecutor slowExecutor;
	private final JobExecutor fastExecutor;


	public JobManager(String labelSuffix) {
		slowExecutor = new JobExecutor("Job Manager slow " + labelSuffix);
		fastExecutor = new JobExecutor("Job Manager fast " + labelSuffix);
	}

	public void addSlowJob(Job job) {
		log.debug("Added job {}", job.getLabel());
		slowExecutor.add(job);
	}
	
	public void addFastJob(Job job) {
		fastExecutor.add(job);
	}
	
	public List<Job> getSlowJobs() {
		return slowExecutor.getJobs();
	}
	
	@Override
	public void start() throws Exception {
		log.debug("Started Job Manager");
		slowExecutor.start();
		fastExecutor.start();
	}

	@Override
	public void stop() {
		fastExecutor.close();
		slowExecutor.close();
	}
	
	public JobManagerStatus reportStatus() {
		return new JobManagerStatus(
			LocalDateTime.now(),
			getSlowJobs()
				.stream()
				.map(job->new JobStatus(job.getJobId(), job.getProgressReporter(), job.getLabel(), job.isCancelled()))
				.collect(Collectors.toList())
		);
	}

	public boolean isSlowWorkerBusy() {
		return slowExecutor.isBusy();
	}

	public boolean cancelJob(UUID jobId) {
		return fastExecutor.cancelJob(jobId) || slowExecutor.cancelJob(jobId);
	}
}
