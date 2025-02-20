package com.bakdata.conquery.util.progressreporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProgressReporterImpl implements ProgressReporter {

	public long getMax() {
		return max.get();
	}

	private final AtomicLong max = new AtomicLong(1);
	private final AtomicLong innerProgress = new AtomicLong(0);
	private final AtomicLong reservedForChildren = new AtomicLong(0);
	private final List<ProgressReporterImpl> children = new ArrayList<>();

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
		long absoluteProgress = innerProgress.get();

		for (ProgressReporterImpl child : children) {
			absoluteProgress += child.getAbsoluteProgress();
		}
		
		return absoluteProgress;
	}
	
	public long getAbsoluteMax() {
		long absoluteMax = max.get();

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

		reservedForChildren.addAndGet(steps);

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
		if (!isStarted()) {
			log.warn("Progress reporter was not started");
			return;
		}
		if (innerProgress.get() + steps > max.get()) {
			log.warn("Progress({}) + ChildProgressReserve({}) + Steps({}) is bigger than the maximum Progress({}). There might be to many reports in the code.", innerProgress, reservedForChildren, steps, max);
			return;
		}

		innerProgress.addAndGet(steps);
	}

	@Override
	public void setMax(long max) {

		if (max <= 0) {
			log.warn("Max can not be 0 or less");
			return;
		}

		this.max.set(max);
	}

	@Override
	public void done() {
		if (isDone()) {
			log.warn("Done was called again for {}", this);
			return;
		}
		endTimeMillis = System.currentTimeMillis();

		for (ProgressReporter child : children) {
			child.done();
		}

		if (getAbsoluteProgress() < getAbsoluteMax()) {
			log.trace("Done was called before all steps were been reported. There might be missing reporting steps in the code.");
		}

		innerProgress.set(max.get() - reservedForChildren.get());
	}
	

	
	@JsonValue
	public ImmutableProgressReporter toImmutable() {
		return new ImmutableProgressReporter(this);
	}
}
