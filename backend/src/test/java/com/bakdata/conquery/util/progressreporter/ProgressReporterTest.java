package com.bakdata.conquery.util.progressreporter;

import static com.bakdata.conquery.util.progressreporter.ProgressReporterUtil.MAX_PROGRESS;
import static com.bakdata.conquery.util.progressreporter.ProgressReporterUtil.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.jackson.Mappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class ProgressReporterTest {
	private List<Integer> allOccurencesOf(String haystack, String needle) {
		List<Integer> occurences = new ArrayList<>();
		int lastIndex = 0;
		while(lastIndex != -1) {
			lastIndex = haystack.indexOf(needle,lastIndex);
			if(lastIndex != -1){
				occurences.add(lastIndex);
				lastIndex += 1;
			}
		}
		return occurences;
	}

	@Test
	public void alignmentTest() throws InterruptedException {
		ProgressReporterImpl pr = (ProgressReporterImpl)ProgressReporter.createWaiting();
		Thread.sleep(2_000);
		
		pr.start();
		pr.setMax(100);
		pr.report(1);

		List<Integer> occurenceOfHour = allOccurencesOf(pr.getEstimate(), "h");

		for (int i = 0; i < 10; i++) {
			pr.report(1);
			Thread.sleep(100);
			assertThat(occurenceOfHour).isEqualTo(allOccurencesOf(pr.getEstimate(), "h"));
		}
		
	}
	
	@Test
	public void basicTest() throws InterruptedException {
		
		ProgressReporter pr = ProgressReporter.createStarted();
		pr.setMax(100);
		assertThat(pr.getEstimate()).contains(UNKNOWN);
		Thread.sleep(100);

		pr.report(1);
		
		log.info(pr.getEstimate());
		pr.report(99);
		assertThat(pr.isDone()).isFalse();
		pr.done();
		
		assertThat(pr.isDone()).isTrue();
		assertThat(pr.getEstimate()).isEqualTo(MAX_PROGRESS);
	}
	
	@Test
	public void serialisationTest() throws JsonProcessingException, InterruptedException {
		ProgressReporterImpl pr = (ProgressReporterImpl)ProgressReporter.createStarted();
		pr.setMax(100);
		assertThat(pr.getEstimate()).contains(UNKNOWN);
		Thread.sleep(100);
		pr.report(1);
		log.info(pr.getEstimate());
		
		JsonNode json = Mappers.getMapper().valueToTree(pr);
		log.info(json.asText());
		ImmutableProgressReporter deserialized = (ImmutableProgressReporter) Mappers.getMapper().treeToValue(json, ProgressReporter.class);
		
		log.info(deserialized.getEstimate());
		Thread.sleep(100);
		log.info(deserialized.getEstimate());
	}
}
