package com.bakdata.conquery.util.progress.reporter;

public class NullProgressReporter implements ProgressReporter{

	@Override
	public double getProgress() {
		return 0;
	}

	@Override
	public ProgressReporter subJob(double steps) {
		return this;
	}

	@Override
	public String getEstimate() {
		return ProgressReporterImpl.ZERO_PROGRESS;
	}

	@Override
	public void report(double steps) {
	}

	@Override
	public void setMax(double max) {
	}

	@Override
	public void done() {
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public void start() {
		
	}
	
	@Override
	public String toString() {
		return getEstimate();
	}
}
