package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobExecutor extends Thread {

	private final LinkedBlockingDeque<Job> jobs = new LinkedBlockingDeque<>();
	private final AtomicReference<Job> currentJob = new AtomicReference<>();
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final AtomicBoolean busy = new AtomicBoolean(false);
	
	public JobExecutor(String name) {
		super("JobManager Worker "+name);
	}

	public void add(Job job) {
		if(closed.get()) {
			throw new IllegalStateException("Tried to add a job to a closed JobManager");
		}
		jobs.add(job);
	}
	
	public List<Job> getJobs() {
		List<Job> jobs = new ArrayList<>(this.jobs.size()+1);
		Job current = currentJob.get();
		if(current!=null) {
			jobs.add(current);
		}
		jobs.addAll(this.jobs);
		return jobs;
	}
	
	public boolean isBusy() {
		return busy.get();
	}

	public void close() {
		closed.set(true);
		Uninterruptibles.joinUninterruptibly(this);
	}
	
	@Override
	public void run() {
		while(!closed.get()) {
			Job job;
			try {
				while((job =jobs.poll(100, TimeUnit.MILLISECONDS))!=null) {
					busy.set(true);
					currentJob.set(job);
					job.getProgressReporter().start();
					Stopwatch timer = Stopwatch.createStarted();
					try {
						log.trace("{} started job {}", this.getName(), job);
						job.execute();
						log.trace("{} finished job {} within {}", this.getName(), job, timer.stop());
						currentJob.set(null);
					} catch (Throwable e) {
						log.error("Fast Job "+job+" failed", e);
						currentJob.set(null);
					}
				}
				busy.set(false);
			} catch (InterruptedException e) {
				log.warn("Interrupted JobManager polling", e);
			}
		}
	}
}
