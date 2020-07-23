package com.bakdata.conquery.util.progressreporter;

import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.math.DoubleMath;
import lombok.Getter;
import lombok.Setter;

public class ProgressReporterImpl implements ProgressReporter{
	@Getter
	private double max = 1;
	private double innerProgress = 0;
	private double reservedForChildren = 0;
	private final List<ChildProgressReporter> children = new ArrayList<ChildProgressReporter>();
	@Getter
	private final Stopwatch stopwatch = Stopwatch.createStarted();
	@Getter(onMethod_=@Override)
	private boolean started = false;
	private Duration waited;
	@Getter(onMethod_=@Override)
	private boolean done = false;
	
	
	
	@Override
	public void start() {
		if(started) {
			throw new IllegalStateException("Progress Reporter is already started");
		}
		started = true;
		waited = stopwatch.elapsed();
		stopwatch.reset();
		stopwatch.start();
	}
	
	
	@Override
	/*Value between zero and one*/
	public double getProgress() {
		double realProgress = innerProgress;
		for (ChildProgressReporter child: children) {
			realProgress += child.getProgress() * child.externalSteps ;
		}

		return realProgress/max;
	}

	@Override
	public ProgressReporter subJob(double steps) {
		if(!started) {
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
		double totalProgress = getProgress();
		long nanosElapsed = stopwatch.elapsed().getNano();
		return ProgressReporterUtil.buildProgressReportString(done, totalProgress, nanosElapsed, getWaitedNanos());
	}

	@Override
	public void report(double steps) {
		if(innerProgress + reservedForChildren + steps > max) {
			throw new IllegalArgumentException("Progress + Steps is bigger than the Maximum Progress");
		}
		innerProgress += steps;
	}

	@Override
	public void setMax(double max) {
		if(getProgress() > max) {
			throw new IllegalStateException("No modification of Limits allowed after progress has been made");
		}

		if(max <= 0) {
			throw new IllegalArgumentException("Max can not be 0 or less");
		}

		this.max = max;
	}

	@Override
	public void done() {
		stopwatch.stop();
		for (ChildProgressReporter child: children) {
			if(!child.isDone()) {
				throw new IllegalStateException("One or more Children are not done yet");
			}
		}

		innerProgress = max - reservedForChildren;
		done = true;
	}

	
	class ChildProgressReporter extends ProgressReporterImpl{
		@Getter @Setter
		private double externalSteps;
	}


	@Override
	public long getWaitedSeconds() {
		return waited==null?stopwatch.elapsed(TimeUnit.SECONDS):waited.toSeconds();
	}
	
	public long getWaitedNanos() {
		return waited==null?stopwatch.elapsed(TimeUnit.NANOSECONDS):waited.toNanos();
	}
	
	@Override
	// given in Seconds
	public long getStartTime() {
		return DoubleMath.roundToLong(System.currentTimeMillis() / 1_000, RoundingMode.DOWN) - stopwatch.elapsed(TimeUnit.SECONDS);
	}
}
