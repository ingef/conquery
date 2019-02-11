package com.bakdata.conquery.models.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.models.common.CDateRange;

public class DateContextTest {

	@Test
	public void rangeAbsCompleteTest() {
		CDateRange mask = new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, null);
		
		List<DateContext> expected = new ArrayList<>();
		DateContext dc = new DateContext(mask);
		dc.setFeatureGroups(EnumSet.of(FeatureGroup.OUTCOME));
		expected.add(dc);
		assertThat(contexts).isEqualTo(expected);
	}
	
	@Test
	public void rangeAbsYearTest() {
		CDateRange mask = new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.YEAR_WISE);
		
		List<CDateRange> expectedRanges = new ArrayList<>();
		expectedRanges.add(new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21)));
		expectedRanges.add(new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 12, 31)));
		expectedRanges.add(new CDateRange(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31)));
		expectedRanges.add(new CDateRange(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 12, 31)));
		expectedRanges.add(new CDateRange(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 12, 31)));
		expectedRanges.add(new CDateRange(LocalDate.of(2005, 1, 1), LocalDate.of(2005, 4, 21)));
		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyInAnyOrderElementsOf(expectedRanges);
		assertThat(contexts).extracting(DateContext::getFeatureGroups).containsOnly(EnumSet.of(FeatureGroup.OUTCOME));
	}
	
	@Test
	public void rangeAbsQuarterTest() {
		CDateRange mask = new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2003, 1, 1));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.QUARTER_WISE);
		
		List<CDateRange> expectedRanges = new ArrayList<>();
		expectedRanges.add(new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2003, 1, 1)));
		expectedRanges.add(new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 6, 30)));
		expectedRanges.add(new CDateRange(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)));
		expectedRanges.add(new CDateRange(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 12, 31)));
		expectedRanges.add(new CDateRange(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 3, 31)));
		expectedRanges.add(new CDateRange(LocalDate.of(2002, 4, 1), LocalDate.of(2002, 6, 30)));
		expectedRanges.add(new CDateRange(LocalDate.of(2002, 7, 1), LocalDate.of(2002, 9, 30)));
		expectedRanges.add(new CDateRange(LocalDate.of(2002, 10, 1), LocalDate.of(2002, 12, 31)));
		expectedRanges.add(new CDateRange(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 1, 1)));
		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyInAnyOrderElementsOf(expectedRanges);
		assertThat(contexts).extracting(DateContext::getFeatureGroups).containsOnly(EnumSet.of(FeatureGroup.OUTCOME));
	}
}
