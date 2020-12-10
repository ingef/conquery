package com.bakdata.conquery.models.forms;

import static com.bakdata.conquery.models.forms.util.DateContextMode.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.models.forms.util.DateContextMode;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import org.junit.jupiter.api.Test;

public class DateContextTest {

	@Test
	public void rangeAbsCompleteTestWithCoarse() {
		// Because COMPLETE_ONLY is the most coarse subdivision mode this has the same output as DateContextTest#rangeAbsCompleteTestWithoutCoarse
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.COMPLETE);

		assertThat(contexts).containsExactlyInAnyOrder(
			new DateContext(mask, FeatureGroup.OUTCOME, null, null, COMPLETE)
		);
	}

	@Test
	public void rangeAbsYearTestWithCoarse() {
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, YEARS.getThisAndCoarserSubdivisions());

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyInAnyOrder (
			mask,
			CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 12, 31)),
			CDateRange.of(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31)),
			CDateRange.of(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 12, 31)),
			CDateRange.of(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 12, 31)),
			CDateRange.of(LocalDate.of(2005, 1, 1), LocalDate.of(2005, 4, 21))
		);
		assertThat(contexts).extracting(DateContext::getFeatureGroup).containsOnly(FeatureGroup.OUTCOME);
	}
	
	@Test
	public void rangeAbsYearTestWithoutCoarse() {
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, YEARS);

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyInAnyOrder (
			CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 12, 31)),
			CDateRange.of(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31)),
			CDateRange.of(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 12, 31)),
			CDateRange.of(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 12, 31)),
			CDateRange.of(LocalDate.of(2005, 1, 1), LocalDate.of(2005, 4, 21))
		);
		assertThat(contexts).extracting(DateContext::getFeatureGroup).containsOnly(FeatureGroup.OUTCOME);
	}

	@Test
	public void rangeAbsQuarterTestWithCoarse() {
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2003, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, QUARTERS.getThisAndCoarserSubdivisions());

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyInAnyOrder (
			// Complete
			mask,
			// Years
			CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 12, 31)),
			CDateRange.of(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31)),
			CDateRange.of(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 4, 21)),			
			// Quarters
			CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 6, 30)),
			CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)),
			CDateRange.of(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 12, 31)),
			CDateRange.of(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 3, 31)),
			CDateRange.of(LocalDate.of(2002, 4, 1), LocalDate.of(2002, 6, 30)),
			CDateRange.of(LocalDate.of(2002, 7, 1), LocalDate.of(2002, 9, 30)),
			CDateRange.of(LocalDate.of(2002, 10, 1), LocalDate.of(2002, 12, 31)),
			CDateRange.of(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 3, 31)),
			CDateRange.of(LocalDate.of(2003, 4, 1), LocalDate.of(2003, 4, 21))
		);
		assertThat(contexts).extracting(DateContext::getFeatureGroup).containsOnly(FeatureGroup.OUTCOME);
	}
	
	@Test
	public void rangeAbsQuarterTestWithoutCoarse() {
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2003, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, QUARTERS);

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyInAnyOrder (
			CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 6, 30)),
			CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)),
			CDateRange.of(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 12, 31)),
			CDateRange.of(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 3, 31)),
			CDateRange.of(LocalDate.of(2002, 4, 1), LocalDate.of(2002, 6, 30)),
			CDateRange.of(LocalDate.of(2002, 7, 1), LocalDate.of(2002, 9, 30)),
			CDateRange.of(LocalDate.of(2002, 10, 1), LocalDate.of(2002, 12, 31)),
			CDateRange.of(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 3, 31)),
			CDateRange.of(LocalDate.of(2003, 4, 1), LocalDate.of(2003, 4, 21))
		);
		assertThat(contexts).extracting(DateContext::getFeatureGroup).containsOnly(FeatureGroup.OUTCOME);
	}

	@Test
	public void rangeAbsYearRelStartQuartAlignTestWithoutCoarse() {
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, YEARS_RELATIVE_TO_START_QUARTER_ALIGNED);

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactly (
				CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2002, 3, 31)),
				CDateRange.of(LocalDate.of(2002, 4, 1), LocalDate.of(2003, 3, 31)),
				CDateRange.of(LocalDate.of(2003, 4, 1), LocalDate.of(2004, 3, 31)),
				CDateRange.of(LocalDate.of(2004, 4, 1), LocalDate.of(2005, 3, 31)),
				CDateRange.of(LocalDate.of(2005, 4, 1), LocalDate.of(2005, 4, 21))
		);
		assertThat(contexts).extracting(DateContext::getFeatureGroup).containsOnly(FeatureGroup.OUTCOME);
	}

	@Test
	public void rangeAbsYearRelEndQuartAlignTestWithoutCoarse() {
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, YEARS_RELATIVE_TO_END_QUARTER_ALIGNED);

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactly (
				CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 6, 30)),
				CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2002, 6, 30)),
				CDateRange.of(LocalDate.of(2002, 7, 1), LocalDate.of(2003, 6, 30)),
				CDateRange.of(LocalDate.of(2003, 7, 1), LocalDate.of(2004, 6, 30)),
				CDateRange.of(LocalDate.of(2004, 7, 1), LocalDate.of(2005, 4, 21))
		);
		assertThat(contexts).extracting(DateContext::getFeatureGroup).containsOnly(FeatureGroup.OUTCOME);
	}

	@Test
	public void rangeRelDaysBeforeTest() {
		List<DateContextMode> resolutions = List.of(COMPLETE,DAYS);
		DateContextMode timeUnit = DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 1;
		IndexPlacement indexPlacement = IndexPlacement.BEFORE;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, resolutions);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 23)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, -1, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 5, 23)), FeatureGroup.FEATURE, 0, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, 1, eventDate, DAYS)
		);
	}
	
	@Test
	public void rangeRelDaysBeforeCompleteOnlyTest() {
		List<DateContextMode> resolutions = List.of(COMPLETE);
		DateContextMode timeUnit = DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 1;
		IndexPlacement indexPlacement = IndexPlacement.BEFORE;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, resolutions);

		List<DateContext> expectedRanges = Arrays.asList(
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 23)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE)
		);
		assertThat(contexts).containsExactlyInAnyOrderElementsOf(expectedRanges);
	}
	
	@Test
	public void rangeRelDaysAfterTest() {
		List<DateContextMode> resolutions = List.of(COMPLETE,DAYS);
		DateContextMode timeUnit = DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.AFTER;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime,  timeUnit,resolutions);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 21)), FeatureGroup.FEATURE, -2, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, -1, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 5, 23)), FeatureGroup.OUTCOME, 0, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, 1, eventDate, DAYS)
		);
	}
	
	@Test
	public void rangeRelDaysNeutralTest() {
		List<DateContextMode> resolutions = List.of(COMPLETE,DAYS);
		DateContextMode timeUnit = DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.NEUTRAL;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, resolutions);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 21)), FeatureGroup.FEATURE, -2, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, -1, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 25)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, 1, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 25), LocalDate.of(2001, 5, 25)), FeatureGroup.OUTCOME, 2, eventDate, DAYS)
		);
	}
	
	@Test
	public void rangeRelQuarterBeforeTest() {
		List<DateContextMode> resolutions = List.of(COMPLETE,QUARTERS);
		DateContextMode timeUnit = QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 1;
		IndexPlacement indexPlacement = IndexPlacement.BEFORE;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, resolutions);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 6, 30)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 4, 1), LocalDate.of(2001, 6, 30)), FeatureGroup.FEATURE, 0, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, 1, eventDate, QUARTERS)
		);
	}
	
	@Test
	public void rangeRelQuarterAfterTest() {
		List<DateContextMode> resolutions = List.of(COMPLETE,QUARTERS);
		DateContextMode timeUnit = QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.AFTER;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, resolutions);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2000, 12, 31)), FeatureGroup.FEATURE, -2, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 4, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 4, 1), LocalDate.of(2001, 6, 30)), FeatureGroup.OUTCOME, 0, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, 1, eventDate, QUARTERS)
		);
	}
	
	@Test
	public void rangeRelQuarterNeutralTest() {
		List<DateContextMode> resolutions = List.of(COMPLETE,QUARTERS);
		DateContextMode timeUnit = QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.NEUTRAL;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, resolutions);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2000, 12, 31)), FeatureGroup.FEATURE, -2, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 12, 31)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, 1, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 12, 31)), FeatureGroup.OUTCOME, 2, eventDate, QUARTERS)
		);
	}
}
