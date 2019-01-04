package com.bakdata.conquery.models.jobs;

import java.util.List;
import java.util.stream.Collectors;

import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobManager implements Managed {
	private final ThreadGroup threadGroup = new ThreadGroup("Job Manager");
	private final JobExecutor slowExecutor = new JobExecutor(threadGroup, "slow");
	private final JobExecutor fastExecutor = new JobExecutor(threadGroup, "fast");

	public void addSlowJob(Job job) {
		log.info("Added job {}", job.getLabel());
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
	public void stop() throws Exception {
		fastExecutor.close();
		slowExecutor.close();
	}
	
	public List<JobStatus> reportStatus() {
		return getSlowJobs()
			.stream()
			.map(job->new JobStatus(job.getProgressReporter(), job.getLabel()))
			.collect(Collectors.toList());
	}
	
	public boolean isSlowWorkerBusy() {
		return slowExecutor.isBusy();
	}
}
