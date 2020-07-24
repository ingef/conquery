package com.bakdata.conquery.util.progressreporter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class ProgressReporterUtilTest {

	@Test
	void buildProgressReportString() {
		assertThat(ProgressReporterUtil.buildProgressReportString(false, 0.2d, TimeUnit.MINUTES.toMillis(2),0)).endsWith("est. 00h 08m 00s ");
		assertThat(ProgressReporterUtil.buildProgressReportString(false, 0.1d, TimeUnit.MINUTES.toMillis(2),0)).endsWith("est. 00h 18m 00s ");
	}
}