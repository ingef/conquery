package com.bakdata.conquery.util.progressreporter;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
@Getter @RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class ImmutableProgressReporter implements ProgressReporter{
	@Getter(onMethod_=@JsonValue)
	private final Values values;
	
	@Data
	public static final class Values {
		private double progress = 0;
		private int max = 0;
		private boolean done = false;
		private boolean started = false;
		private long waitedSeconds;
		private long createdTime;
		private long startTime;
	}
	
	public ImmutableProgressReporter(ProgressReporter pr) {
		values = new Values();
		values.progress = pr.getProgress();
		values.startTime = pr.getStartTime();
		values.done = pr.isDone();
		values.started = pr.isStarted();
		values.max = pr.getMax();

		if(!values.started) {
			values.createdTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - pr.getWaitedSeconds();
		}
	}

	@Override
	public String getEstimate() {
		long elapsed = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - values.startTime;
		return ProgressReporterUtil.buildProgressReportString(values.done, values.progress, elapsed, values.waitedSeconds);
	}
	
	@Override @JsonProperty
	public long getWaitedSeconds() {
		if(values.started) {
			return values.waitedSeconds;
		} else {
			return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - values.createdTime;
		}
	}
	
	@Override
	public void start() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ProgressReporter subJob(int steps) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void report(int steps) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMax(int max) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void done() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getStartTime() {
		return values.getStartTime();
	}

	@Override
	public int getMax() {
		return values.getMax();
	}

	@Override
	public boolean isStarted() {
		return values.isStarted();
	}

	@Override
	public double getProgress() {
		return values.getProgress();
	}

	@Override
	public boolean isDone() {
		return values.isDone();
	}
}
