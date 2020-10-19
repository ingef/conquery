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
	default ImmutableProgressReporter toImmutable() {
		return new ImmutableProgressReporter(this);
	}
	
	long getStartTimeMillis();
	boolean isStarted();


	double getProgress();
	long getAbsoluteProgress();

	ProgressReporter subJob(long steps);
	String getEstimate();

	void report(int steps);

	void setMax(long max);
	long getMax();

	void done();
	boolean isDone();

	long getCreationTimeMillis();
}
