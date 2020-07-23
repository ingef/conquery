package com.bakdata.conquery.util.progressreporter;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
@JsonDeserialize(as = ImmutableProgressReporter.class)
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
	
	@JsonValue
	default ImmutableProgressReporter.Values toImmutable() {
		return new ImmutableProgressReporter(this).getValues();
	}
	
	long getWaitedSeconds();
	long getStartTime();
	boolean isStarted();
	
	
	double getProgress();
	ProgressReporter subJob(double steps);
	String getEstimate();
	void report(double steps);
	void setMax(double max);
	double getMax();
	void done();
	boolean isDone();
}
