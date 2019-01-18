package com.bakdata.conquery.util.progressreporter;

import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.math.DoubleMath;

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
		private boolean done = false;
		private boolean started = false;
		private long waitedSeconds;
		private long createdTime;
		private long startTime;
	}
	
	public ImmutableProgressReporter(ProgressReporter pr) {
		values = new Values();
		values.progress = pr.getProgress();
		values.startTime = DoubleMath.roundToLong(System.currentTimeMillis()/1_000, RoundingMode.DOWN) - pr.getStartTime();
		values.done = pr.isDone();
		values.started = pr.isStarted();
		if(!values.started) {
			values.createdTime = DoubleMath.roundToLong(System.currentTimeMillis()/1_000, RoundingMode.DOWN) - pr.getWaitedSeconds();
		}
	}

	@Override
	public String getEstimate() {
		long elapsed = System.nanoTime() - values.startTime;
		return ProgressReporterUtil.buildProgressReportString(values.done, values.progress, elapsed, values.waitedSeconds * 1_000_000_000);
	}
	
	@Override @JsonProperty
	public long getWaitedSeconds() {
		if(values.started) {
			return values.waitedSeconds;
		} else {
			return DoubleMath.roundToLong(System.currentTimeMillis()/1_000, RoundingMode.DOWN) - values.createdTime;
		}
	}
	
	@Override
	public void start() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ProgressReporter subJob(double steps) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void report(double steps) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMax(double max) {
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
