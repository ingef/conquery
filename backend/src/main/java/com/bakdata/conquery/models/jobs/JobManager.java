package com.bakdata.conquery.models.jobs;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobManager {
	private final JobExecutor slowExecutor;
	private final JobExecutor fastExecutor;
	private final String name;


	public JobManager(String name) {
		this.name = name;
		slowExecutor = new JobExecutor("Job Manager slow " + this.name);
		fastExecutor = new JobExecutor("Job Manager fast " + this.name);

		slowExecutor.start();
		fastExecutor.start();
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

	public void stop() throws Exception {
		fastExecutor.close();
		slowExecutor.close();
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
}
