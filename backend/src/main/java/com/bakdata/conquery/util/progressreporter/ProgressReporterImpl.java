package com.bakdata.conquery.util.progressreporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProgressReporterImpl implements ProgressReporter {


	@Getter(onMethod_ = @Override)
	private long max = 1;
	private long innerProgress = 0;
	private long reservedForChildren = 0;
	private final List<ProgressReporterImpl> children = new ArrayList<ProgressReporterImpl>();

	@Getter
	private final long creationTimeMillis;
	@Getter
	private long startTimeMillis = -1;
	private long endTimeMillis = -1;

	public ProgressReporterImpl(){
		creationTimeMillis = System.currentTimeMillis();
	}

	@Override
	public void start() {
		if (isStarted()) {
			log.warn("Progress Reporter is already started");
		}

		startTimeMillis = System.currentTimeMillis();
	}

	@Override
	public boolean isStarted() {
		return startTimeMillis > 0;
	}

	@Override
	public boolean isDone() {
		return endTimeMillis > 0;
	}

	@Override
	/*Value between zero and one*/
	public double getProgress() {
		return (double) getAbsoluteProgress() / (double) getAbsoluteMax();
	}
	
	public long getAbsoluteProgress() {
		long absoluteProgress = innerProgress;

		for (ProgressReporterImpl child : children) {
			absoluteProgress += child.getAbsoluteProgress();
		}
		
		return absoluteProgress;
	}
	
	public long getAbsoluteMax() {
		long absoluteMax = max;

		for (ProgressReporterImpl child : children) {
			absoluteMax += child.getAbsoluteMax();
		}
		
		return absoluteMax;
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

		ProgressReporterImpl childPr = new ProgressReporterImpl();
		childPr.start();
		childPr.setMax(steps);
		children.add(childPr);
		return childPr;
	}

	@Override
	public String getEstimate() {
		return ProgressReporterUtil.buildProgressReportString(isDone(), getAbsoluteProgress(), getAbsoluteMax(), System.currentTimeMillis() - startTimeMillis, startTimeMillis - creationTimeMillis);
	}

	@Override
	public void report(int steps) {
		if (innerProgress + reservedForChildren + steps > max) {
			log.warn("Progress({}) + ChildProgressReserve({}) + Steps({}) is bigger than the maximum Progress({}). There might be to many reports in the code.", innerProgress, reservedForChildren, steps, max);
			return;
		}

		innerProgress += steps;
	}

	@Override
	public void setMax(long max) {
		if (this.max > max) {
			log.warn("Max cannot be decreased.");
			return;
		}

		if (max <= 0) {
			throw new IllegalArgumentException("Max can not be 0 or less");
		}

		this.max = max;
	}

	@Override
	public void done() {
		if(endTimeMillis > -1) {
			log.warn("Done was called again for {}", this);
			return;
		}
		endTimeMillis = System.currentTimeMillis();

		for (ProgressReporter child : children) {
			if (!child.isDone()) {
				log.warn("One or more Children are not done yet");
			}
		}
		
		if(getAbsoluteProgress()<max) {
			log.trace("Done was called before all steps were been reported. There might be missing reporting steps in the code.");
		}

		innerProgress = max - reservedForChildren;
	}
}
