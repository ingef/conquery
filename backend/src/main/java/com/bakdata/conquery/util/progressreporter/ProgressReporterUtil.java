package com.bakdata.conquery.util.progressreporter;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import com.google.common.math.DoubleMath;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProgressReporterUtil {

	private final static DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral("h ").appendValue(ChronoField.MINUTE_OF_HOUR, 2)
			.appendLiteral("m ").appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral("s ").toFormatter();

	/* package */ final static String UNKNOWN = "unknown     ";
	/* package */ final static String MAX_PROGRESS = "done";

	/* package */ String buildProgressReportString(boolean done, double progress, long elapsed, long waited) {
		if (progress == 0) {
			return new StringBuilder().append("waited ")
				.append(ProgressReporterUtil.TIME_FORMATTER
						.format(LocalTime.MIDNIGHT.plus(Duration.ofNanos(waited))))
				.append(String.format("- %3d%% - est. "+UNKNOWN, 0))
				.toString();
		} else if (done) {
			return ProgressReporterUtil.MAX_PROGRESS;
		} else {
			long nanosEstimated = DoubleMath.roundToLong((1+elapsed) / (1+progress - elapsed), RoundingMode.HALF_UP);
			Duration estimate = Duration.ofNanos(nanosEstimated);
			int percent = DoubleMath.roundToInt(progress * 100, RoundingMode.FLOOR);

			return new StringBuilder().append("waited ")
					.append(ProgressReporterUtil.TIME_FORMATTER
							.format(LocalTime.MIDNIGHT.plus(Duration.ofNanos(waited))))
					.append(String.format("- %3d%% - est. ", percent))
					.append(ProgressReporterUtil.TIME_FORMATTER.format(LocalTime.MIDNIGHT.plus(estimate))).toString();
		}
	}
}
