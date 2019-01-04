package com.bakdata.conquery.util.progress.reporter;

import static com.bakdata.conquery.util.progress.reporter.ProgressReporterImpl.MAX_PROGRESS;
import static com.bakdata.conquery.util.progress.reporter.ProgressReporterImpl.ZERO_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProgressReporterTest {
	private List<Integer> allOccurencesOf(String haystack, String needle) {
		List<Integer> occurences = new ArrayList<>();
		int lastIndex = 0;
		while (lastIndex != -1) {
			lastIndex = haystack.indexOf(needle, lastIndex);
			if (lastIndex != -1) {
				occurences.add(lastIndex);
				lastIndex += 1;
			}
		}
		return occurences;
	}

	@Test
	public void alignmentTest() throws InterruptedException {
		ProgressReporterImpl pr = (ProgressReporterImpl) ProgressReporter.createWaiting();
		Thread.sleep(2_000);

		pr.start();
		pr.setMax(100d);
		pr.report(1d);
		List<Integer> occurenceOfHour = allOccurencesOf(pr.getEstimate(), "h");
		for (int i = 0; i < 99; i++) {
			pr.report(1d);
			Thread.sleep(100);
			log.info(pr.getEstimate());
			assertThat(occurenceOfHour).isEqualTo(allOccurencesOf(pr.getEstimate(), "h"));
		}

	}

	@Test
	public void basicTest() throws IOException, InterruptedException {

		ProgressReporterImpl pr = (ProgressReporterImpl) ProgressReporter.createStarted();
		pr.setMax(100d);
		assertThat(pr.getEstimate()).isEqualTo(ZERO_PROGRESS);
		log.info(pr.getStopwatch().toString());
		Thread.sleep(100);
		pr.report(1);
		log.info(pr.getStopwatch().toString());
		log.info(pr.getEstimate());
		Thread.sleep(10000);
		log.info(pr.getEstimate());

		log.info(pr.getStopwatch().toString());
		pr.report(99);
		assertThat(pr.isDone()).isFalse();
		pr.done();

		assertThat(pr.isDone()).isTrue();
		assertThat(pr.getEstimate()).isEqualTo(MAX_PROGRESS);
	}
}
