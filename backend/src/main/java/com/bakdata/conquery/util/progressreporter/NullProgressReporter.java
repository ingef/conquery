package com.bakdata.conquery.util.progressreporter;

import java.math.RoundingMode;

import com.google.common.math.DoubleMath;

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
		return ProgressReporterUtil.UNKNOWN;
	}

	@Override
	public void report(double steps) {
	}

	@Override
	public void setMax(double max) {
	}

	@Override
	public double getMax() {
		return 0;
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
	public long getWaitedSeconds() {
		return 0;
	}

	@Override
	public long getStartTime() {
		return DoubleMath.roundToLong(System.currentTimeMillis()/1_000, RoundingMode.DOWN);
	}

	@Override
	public boolean isStarted() {
		return false;
	}
}
