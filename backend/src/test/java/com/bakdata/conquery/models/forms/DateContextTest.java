package com.bakdata.conquery.models.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;

public class DateContextTest {

	@Test
	public void rangeAbsCompleteTest() {
		CDateRange mask = new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.COMPLETE_ONLY);

		DateContext dc = new DateContext(mask);
		dc.setFeatureGroups(EnumSet.of(FeatureGroup.OUTCOME));
		assertThat(contexts).isEqualTo(Arrays.asList(dc));
	}

	@Test
	public void rangeAbsYearTest() {
		CDateRange mask = new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.YEAR_WISE);

		List<CDateRange> expectedRanges = Arrays
			.asList(
				mask,
				new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 12, 31)),
				new CDateRange(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31)),
				new CDateRange(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 12, 31)),
				new CDateRange(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 12, 31)),
				new CDateRange(LocalDate.of(2005, 1, 1), LocalDate.of(2005, 4, 21)));

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyElementsOf(expectedRanges);
		assertThat(contexts).extracting(DateContext::getFeatureGroups).containsOnly(EnumSet.of(FeatureGroup.OUTCOME));
	}

	@Test
	public void rangeAbsQuarterTest() {
		CDateRange mask = new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2003, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.QUARTER_WISE);

		List<CDateRange> expectedRanges = Arrays
			.asList(
				mask,
				new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 6, 30)),
				new CDateRange(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)),
				new CDateRange(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 12, 31)),
				new CDateRange(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 3, 31)),
				new CDateRange(LocalDate.of(2002, 4, 1), LocalDate.of(2002, 6, 30)),
				new CDateRange(LocalDate.of(2002, 7, 1), LocalDate.of(2002, 9, 30)),
				new CDateRange(LocalDate.of(2002, 10, 1), LocalDate.of(2002, 12, 31)),
				new CDateRange(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 3, 31)),
				new CDateRange(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 4, 21)));

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyElementsOf(expectedRanges);
		assertThat(contexts).extracting(DateContext::getFeatureGroups).containsOnly(EnumSet.of(FeatureGroup.OUTCOME));
	}

	@Test
	public void rangeRelCompleteTest() {
		Resolution resolution = Resolution.YEARS;
		DateContextMode resultMode = DateContextMode.YEAR_WISE;
		int event = CDate.ofLocalDate(LocalDate.of(2001, 5, 23)); //because of the yearwise resolution that means that whole 2001 is our event range
		int featureTime = 2; //we want two features years (while one is in this case the event year itself)
		int outcomeTime = 1;
		EventIndex eventIndex = EventIndex.FEATURE; //meaning the feature year(because of the yearwise resolution) itself should be counted to the feature time
		//if eventIndex was NEITHER we would want two additional years before the event year as the feature range and one year afterwards
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, eventIndex, featureTime, outcomeTime, resultMode);

		List<CDateRange> expectedRanges = Arrays.asList(											  // V this is the index (Integer) which is always null in the absolute case
			new CDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2001, 12, 31)), // complete feature null -> FeatureGroup FEATURE
			new CDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2000, 12, 31)), // feature  			-1 -> FeatureGroup FEATURE
			new CDateRange(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 12, 31)), // feature   			 0 -> FeatureGroup FEATURE
			new CDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2002, 12, 31)), // complete outcome null -> FeatureGroup OUTCOME
			new CDateRange(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31))  // outcome  			+1 -> FeatureGroup OUTCOM
		);
		assertThat(contexts).isEqualTo(expectedRanges);
		
		/*
		 Du kannst relativ modellieran als:
		 	1. bestimme den eventzeitraum mit eventTag + resolution 	=> eventRange
		 	2. bestimme featureRange durch featureTime + resolution
		 		a) endet entweder vor eventRange oder mit dem Ende von EventRange je nach eventIdex
		 		b) startet featureTime Zeiteinheiten davor
		 		c) benutze absolute Logik um resultMode aus diesem Bereich zu machen
		 	2. bestimme outcomeRange durch outcomeTime + resolution
		 		a) startet entweder nach eventRange oder mit dem Anfang von EventRange je nach eventIdex
		 		b) endet outcomeTime Zeiteinheiten danach
		 		c) benutze absolute Logik um resultMode aus diesem Bereich zu machen
		 	3. setze Indizes
		 		eventRange hat 0 falls vorhanden
		 		alle anderen haben ihren offset in Zeiteinheiten
		 */
	}
}
