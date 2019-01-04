package com.bakdata.conquery.util.progress.reporter;

public interface ProgressReporter {
	public static ProgressReporter createStarted() {
		ProgressReporter pr = new ProgressReporterImpl();
		pr.start();
		return pr;
	}
	public static ProgressReporter createWaiting() {
		return new ProgressReporterImpl();
	}
	
	public void start();
	
	double getProgress();
	ProgressReporter subJob(double steps);
	String getEstimate();
	void report(double steps);
	void setMax(double max);
	void done();
	boolean isDone();
}
