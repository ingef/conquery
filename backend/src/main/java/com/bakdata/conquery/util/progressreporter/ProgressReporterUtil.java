package com.bakdata.conquery.util.progressreporter;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import com.google.common.math.DoubleMath;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ProgressReporterUtil {

	private final static DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral("h ").appendValue(ChronoField.MINUTE_OF_HOUR, 2)
			.appendLiteral("m ").appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral("s ").toFormatter();

	/* package */ final static String UNKNOWN = "unknown     ";
	/* package */ final static String MAX_PROGRESS = "done";

	/* package */ String buildProgressReportString(boolean done, double progress, long elapsedMillis, long waitedMillis) {
		if (done) {
			return ProgressReporterUtil.MAX_PROGRESS;
		}
		
		boolean canEstimate = true;
		long estimateMillis = 0;
		try {			
			estimateMillis = DoubleMath.roundToLong(elapsedMillis / progress - elapsedMillis, RoundingMode.HALF_UP);
		}catch (Exception e) {
			log.warn("Could not estimate the progress for the current state: done({}), progress({}), elapsedMillis({}), waitedMillis({})", done, progress, elapsedMillis, waitedMillis,e);
			canEstimate = false;
		}

		if (progress == 0 || !canEstimate) {
			return String.format(
					"waited %s - %3d%% - est. %s",
					ProgressReporterUtil.TIME_FORMATTER.format(LocalTime.MIDNIGHT.plus(Duration.ofMillis(waitedMillis))),
					0,
					UNKNOWN
			);
		}


		Duration estimate = Duration.ofMillis(estimateMillis);

		int percent = DoubleMath.roundToInt(progress * 100, RoundingMode.FLOOR);

		return String.format("waited %s - %3d%% - est. %s",
							 ProgressReporterUtil.TIME_FORMATTER.format(LocalTime.MIDNIGHT.plus(Duration.ofMillis(waitedMillis))),
							 percent,
							 ProgressReporterUtil.TIME_FORMATTER.format(LocalTime.MIDNIGHT.plus(estimate)));
	}
}
