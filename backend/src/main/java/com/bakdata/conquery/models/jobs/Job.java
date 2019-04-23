package com.bakdata.conquery.models.jobs;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bakdata.conquery.util.progressreporter.ProgressReporter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor @Getter
public abstract class Job {
	@Getter
	private final UUID jobId = UUID.randomUUID();

	@Setter
	protected ProgressReporter progressReporter = ProgressReporter.createWaiting();

	private AtomicBoolean cancelledState = new AtomicBoolean(false);

	public void cancel() {
		cancelledState.set(true);
	}

	public boolean isCancelled() {
		return cancelledState.get();
	}


	public abstract void execute() throws Exception;
	public abstract String getLabel();

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"(progress="+progressReporter.getEstimate()+")";
	}
}
