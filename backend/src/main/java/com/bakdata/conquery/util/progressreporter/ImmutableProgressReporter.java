package com.bakdata.conquery.util.progressreporter;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
@Getter @RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class ImmutableProgressReporter implements ProgressReporter{
	private long absoluteProgress = 0;
	private long max = 0;
	private boolean done = false;
	private boolean started = false;
	private long creationTimeMillis; //millis
	private long startTimeMillis; //millis
	
	public ImmutableProgressReporter(ProgressReporter pr) {
		absoluteProgress = pr.getAbsoluteProgress();
		startTimeMillis = pr.getStartTimeMillis();
		done = pr.isDone();
		started = pr.isStarted();
		max = pr.getMax();
		creationTimeMillis = pr.getCreationTimeMillis();
	}

	@Override
	public String getEstimate() {
		long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
		long waitedMillis = System.currentTimeMillis() - creationTimeMillis;
		return ProgressReporterUtil.buildProgressReportString(done, absoluteProgress, max, elapsedMillis, waitedMillis);
	}
	
	@JsonIgnore
	public long getWaitedSeconds() {
		return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - creationTimeMillis);
	}
	
	@Override
	public void start() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ProgressReporter subJob(long steps) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void report(int steps) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMax(long max) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void done() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getProgress() {
		if(max <= 0) {
			return 0;
		}
		return ((double)absoluteProgress)/max;
	}
}
