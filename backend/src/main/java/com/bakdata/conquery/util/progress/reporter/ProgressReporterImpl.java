package com.bakdata.conquery.util.progress.reporter;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Stopwatch;
import com.google.common.math.DoubleMath;

import lombok.Getter;
import lombok.Setter;

public class ProgressReporterImpl implements ProgressReporter {
	private double max = 1;
	private double innerProgress = 0;
	private double reservedForChildren = 0;
	private final List<ChildProgressReporter> children = new ArrayList<ChildProgressReporter>();
	@Getter
	private final Stopwatch stopwatch = Stopwatch.createStarted();
	private boolean started = false;
	private Duration waited;
	@Getter(onMethod_ = @Override)
	private boolean done = false;

	private final static DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral("h ").appendValue(ChronoField.MINUTE_OF_HOUR, 2)
			.appendLiteral("m ").appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral("s ").toFormatter();

	public final static String ZERO_PROGRESS = "unknown";
	public final static String MAX_PROGRESS = "done";

	@Override
	public void start() {
		if (started) {
			throw new IllegalStateException("Progress Reporter is already started");
		}
		started = true;
		waited = stopwatch.elapsed();
		stopwatch.reset();
		stopwatch.start();
	}

	@Override
	/* Value between zero and one */
	public double getProgress() {
		double realProgress = innerProgress;
		for (ChildProgressReporter child : children) {
			realProgress += child.getProgress() * child.externalSteps;
		}

		return realProgress / max;
	}

	@Override
	public ProgressReporter subJob(double steps) {
		if (!started) {
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

		if (totalProgress == 0) {
			return ZERO_PROGRESS;
		} else if (done) {
			return MAX_PROGRESS;
		} else {
			long nanosElapsed = stopwatch.elapsed().getNano() + stopwatch.elapsed().getSeconds() * 1_000_000_000;
			long nanosEstimated = DoubleMath.roundToLong((nanosElapsed / totalProgress) - nanosElapsed,
					RoundingMode.HALF_UP);
			Duration estimate = Duration.ofNanos(nanosEstimated);
			int percent = DoubleMath.roundToInt(totalProgress * 100, RoundingMode.FLOOR);

			return new StringBuilder().append("waited ").append(TIME_FORMATTER.format(LocalTime.MIDNIGHT.plus(waited)))
					.append(String.format("- %3d%% - est. ", percent))
					.append(TIME_FORMATTER.format(LocalTime.MIDNIGHT.plus(estimate))).toString();
		}
	}

	@Override
	public void report(double steps) {
		if (innerProgress + reservedForChildren + steps > max) {
			throw new IllegalArgumentException("Progress + Steps is bigger than the Maximum Progress");
		}
		innerProgress += steps;
	}

	@Override
	public void setMax(double max) {
		if (getProgress() != 0) {
			throw new IllegalStateException("No modification of Limits allowed after progress has been made");
		}
		if (max <= 0) {
			throw new IllegalArgumentException("Max can not be 0 or less");
		}
		this.max = max;
	}

	@Override
	public void done() {
		stopwatch.stop();
		for (ChildProgressReporter child : children) {
			if (!child.isDone()) {
				throw new IllegalStateException("One or more Children are not done yet");

			}
		}
		innerProgress = max - reservedForChildren;
		done = true;
	}
	
	@Override
	public String toString() {
		return getEstimate();
	}

	class ChildProgressReporter extends ProgressReporterImpl {
		@Getter
		@Setter
		private double externalSteps;
	}

}
