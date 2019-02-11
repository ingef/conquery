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
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.COMPLETE);
		
		DateContext dc = new DateContext(mask);
		dc.setFeatureGroups(EnumSet.of(FeatureGroup.OUTCOME));
		assertThat(contexts).isEqualTo(Arrays.asList(dc));
	}
	
	@Test
	public void rangeAbsYearTest() {
		CDateRange mask = new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.YEAR_WISE);
		
		List<CDateRange> expectedRanges = Arrays.asList(
			mask,
			new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 12, 31)),
			new CDateRange(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31)),
			new CDateRange(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 12, 31)),
			new CDateRange(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 12, 31)),
			new CDateRange(LocalDate.of(2005, 1, 1), LocalDate.of(2005, 4, 21))
		);
		
		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyElementsOf(expectedRanges);
		assertThat(contexts).extracting(DateContext::getFeatureGroups).containsOnly(EnumSet.of(FeatureGroup.OUTCOME));
	}
	
	@Test
	public void rangeAbsQuarterTest() {
		CDateRange mask = new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2003, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.QUARTER_WISE);
		
		List<CDateRange> expectedRanges = Arrays.asList(
			mask,
			new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 6, 30)),
			new CDateRange(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)),
			new CDateRange(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 12, 31)),
			new CDateRange(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 3, 31)),
			new CDateRange(LocalDate.of(2002, 4, 1), LocalDate.of(2002, 6, 30)),
			new CDateRange(LocalDate.of(2002, 7, 1), LocalDate.of(2002, 9, 30)),
			new CDateRange(LocalDate.of(2002, 10, 1), LocalDate.of(2002, 12, 31)),
			new CDateRange(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 3, 31)),
			new CDateRange(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 4, 21))
		);
		
		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyElementsOf(expectedRanges);
		assertThat(contexts).extracting(DateContext::getFeatureGroups).containsOnly(EnumSet.of(FeatureGroup.OUTCOME));
	}
	
	@Test
	public void rangeRelCompleteTest() {
		CDateRange event = new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2003, 1, 1));
		int daysBefore = 2;
		int daysAfter = 1;
		?? eventIndex = FIRST; // LAST, RANDOM
		DateContextMode resultMode = DateContextMode.COMPLETE_ONLY;
		
		List<DateContext> contexts = DateContext.generateRelativeContexts(
			event,
			eventIndex,
			daysBefore,
			daysAfter,
			resultMode);
		
		List<CDateRange> expectedRanges = new ArrayList<>();
		expectedRanges.add(new CDateRange(LocalDate.of(2001, 5, 21), LocalDate.of(2003, 1, 2))); // complete -> FeatureGroup OUTCOME ?
		expectedRanges.add(new CDateRange(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 22))); // before only -> FeatureGroup FEATURE
		expectedRanges.add(new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2003, 1, 1))); // event only eventIndex = FIRST -> FeatureGroup FEATURE
		expectedRanges.add(new CDateRange(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 1, 2))); // after only -> FeatureGroup OUTCOME
		assertThat(contexts).isEqualTo(expectedRanges);
	}
}
