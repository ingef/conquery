package com.bakdata.conquery.util.progressreporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.math.DoubleMath;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProgressReporterImpl implements ProgressReporter {


	@Getter(onMethod_ = @Override)
	private long max = 1;
	private long innerProgress = 0;
	private long reservedForChildren = 0;
	private final List<ChildProgressReporter> children = new ArrayList<ChildProgressReporter>();

	private final long waitBegin;
	private long begin = -1;
	private long end = -1;

	public ProgressReporterImpl(){
		waitBegin = System.currentTimeMillis();
	}

	@Override
	public void start() {
		if (isStarted()) {
			throw new IllegalStateException("Progress Reporter is already started");
		}

		begin = System.currentTimeMillis();
	}

	@Override
	public boolean isStarted() {
		return begin > 0;
	}

	@Override
	public boolean isDone() {
		return end > 0;
	}

	@Override
	/*Value between zero and one*/
	public double getProgress() {
		long realProgress = innerProgress;

		for (ChildProgressReporter child : children) {
			realProgress += child.getProgress() * child.externalSteps;
		}

		return (double) realProgress / (double) max;
	}

	@Override
	public ProgressReporter subJob(long steps) {
		if (!isStarted()) {
			throw new IllegalStateException("You need to start the Progress Reporter before you can add subjobs");
		}
		if (innerProgress + reservedForChildren + steps > max) {
			throw new IllegalArgumentException("Progress + Steps is bigger than the Maximum Progress");
		}
		reservedForChildren += steps;

		ChildProgressReporter childPr = new ChildProgressReporter();
		childPr.start();
		childPr.setExternalSteps(steps);
		children.add(childPr);
		return childPr;
	}

	@Override
	public String getEstimate() {
		return ProgressReporterUtil.buildProgressReportString(isDone(), getProgress(), System.currentTimeMillis() - begin, begin - waitBegin);
	}

	@Override
	public void report(int steps) {
		if (innerProgress + reservedForChildren + steps > max) {
			throw new IllegalArgumentException("Progress + Steps is bigger than the Maximum Progress");
		}

		innerProgress += steps;
	}

	@Override
	public void setMax(long max) {
		if (getProgress() > max) {
			throw new IllegalStateException("Max cannot be less than already made progress.");
		}

		if (max <= 0) {
			throw new IllegalArgumentException("Max can not be 0 or less");
		}

		this.max = max;
	}

	@Override
	public void done() {
		end = System.currentTimeMillis();

		for (ChildProgressReporter child : children) {
			if (!child.isDone()) {
				throw new IllegalStateException("One or more Children are not done yet");
			}
		}

		// Some numerical error is acceptable here.
		final double progress = getProgress();
		if (DoubleMath.fuzzyEquals(progress,1,0.1d)) {
			log.warn("ProgressReporter is done but Progress is just {}", progress);
		}

		innerProgress = max - reservedForChildren;
	}


	@Data
	private static class ChildProgressReporter extends ProgressReporterImpl {
		private long externalSteps;
	}

	@Override
	public long getWaitedSeconds() {
		return TimeUnit.MILLISECONDS.toSeconds(begin - waitBegin);
	}

	@Override
	// given in Seconds
	public long getStartTime() {
		return TimeUnit.MILLISECONDS.toSeconds(begin);
	}
}
