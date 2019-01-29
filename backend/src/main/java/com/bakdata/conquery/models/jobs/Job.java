package com.bakdata.conquery.models.jobs;

import com.bakdata.conquery.util.progressreporter.ProgressReporter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor @Getter
public abstract class Job {
	@Setter
	protected ProgressReporter progressReporter = ProgressReporter.createWaiting();
	

	public abstract void execute() throws Exception;
	public abstract String getLabel();

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"(progress="+progressReporter.getEstimate()+")";
	}
}
