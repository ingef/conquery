package com.bakdata.conquery.models.forms;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.forms.util.DateContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.bakdata.conquery.models.forms.util.Alignment.*;
import static com.bakdata.conquery.models.forms.util.Resolution.*;
import static org.assertj.core.api.Assertions.assertThat;

public class DateContextTest {

	@Test
	public void rangeAbsCompleteTestWithCoarse() {
		// Because COMPLETE_ONLY is the most coarse subdivision mode this has the same output as DateContextTest#rangeAbsCompleteTestWithoutCoarse
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, List.of(ExportForm.ResolutionAndAlignment.of(COMPLETE, NO_ALIGN)));

		assertThat(contexts).containsExactly(
			new DateContext(mask, FeatureGroup.OUTCOME, null, null, COMPLETE)
		);
	}

	@Test
	public void rangeAbsYearTestWithCoarse() {
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, ExportForm.getResolutionAlignmentMap(YEARS.getThisAndCoarserSubdivisions(),YEAR));

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactly (
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
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, ExportForm.getResolutionAlignmentMap(List.of(YEARS),YEAR));

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactly (
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
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, ExportForm.getResolutionAlignmentMap(QUARTERS.getThisAndCoarserSubdivisions(), QUARTER));

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactly (
			// Complete
			mask,
			// Years
			CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2002, 3, 31)),
			CDateRange.of(LocalDate.of(2002, 4, 1), LocalDate.of(2003, 3, 31)),
			CDateRange.of(LocalDate.of(2003, 4, 1), LocalDate.of(2003, 4, 21)),
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
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, List.of(ExportForm.ResolutionAndAlignment.of(QUARTERS, QUARTER)));

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactly (
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
	public void rangeRelDaysBeforeTest() {
		CalendarUnit timeUnit = CalendarUnit.DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 1;
		IndexPlacement indexPlacement = IndexPlacement.BEFORE;


		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, List.of(ExportForm.ResolutionAndAlignment.of(COMPLETE, NO_ALIGN), ExportForm.ResolutionAndAlignment.of(DAYS, DAY)));

		assertThat(contexts).containsExactly (
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 23)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, -1, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 5, 23)), FeatureGroup.FEATURE, 0, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, 1, eventDate, DAYS)
		);
	}

	@Test
	public void rangeRelDaysBeforeCompleteOnlyTest() {
		CalendarUnit timeUnit = CalendarUnit.DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 1;
		IndexPlacement indexPlacement = IndexPlacement.BEFORE;


		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, List.of(ExportForm.ResolutionAndAlignment.of(COMPLETE, NO_ALIGN)));

		List<DateContext> expectedRanges = Arrays.asList(
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 23)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE)
		);
		assertThat(contexts).containsExactlyElementsOf(expectedRanges);
	}

	@Test
	public void rangeRelDaysAfterTest() {
		CalendarUnit timeUnit = CalendarUnit.DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.AFTER;


		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime,  timeUnit, ExportForm.getResolutionAlignmentMap(List.of(COMPLETE,DAYS), DAY));

		assertThat(contexts).containsExactly (
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 21)), FeatureGroup.FEATURE, -2, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, -1, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 5, 23)), FeatureGroup.OUTCOME, 0, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, 1, eventDate, DAYS)
		);
	}

	@Test
	public void rangeRelDaysNeutralTest() {
		CalendarUnit timeUnit = CalendarUnit.DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.NEUTRAL;


		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, ExportForm.getResolutionAlignmentMap(List.of(COMPLETE,DAYS), DAY));

		assertThat(contexts).containsExactly (
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 25)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 21)), FeatureGroup.FEATURE, -2, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, -1, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, 1, eventDate, DAYS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 25), LocalDate.of(2001, 5, 25)), FeatureGroup.OUTCOME, 2, eventDate, DAYS)
		);
	}

	@Test
	public void rangeRelQuarterBeforeTest() {
		CalendarUnit timeUnit = CalendarUnit.QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 1;
		IndexPlacement indexPlacement = IndexPlacement.BEFORE;


		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, ExportForm.getResolutionAlignmentMap(List.of(COMPLETE,QUARTERS), QUARTER));

		assertThat(contexts).containsExactly(
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 6, 30)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 4, 1), LocalDate.of(2001, 6, 30)), FeatureGroup.FEATURE, 0, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, 1, eventDate, QUARTERS)
		);
	}

	@Test
	public void rangeRelQuarterAfterTest() {
		CalendarUnit timeUnit = CalendarUnit.QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.AFTER;


		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, ExportForm.getResolutionAlignmentMap(List.of(COMPLETE,QUARTERS), QUARTER));

		assertThat(contexts).containsExactly (
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 4, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2000, 12, 31)), FeatureGroup.FEATURE, -2, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 4, 1), LocalDate.of(2001, 6, 30)), FeatureGroup.OUTCOME, 0, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, 1, eventDate, QUARTERS)
		);
	}

	@Test
	public void rangeRelQuarterNeutralTest() {
		CalendarUnit timeUnit = CalendarUnit.QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.NEUTRAL;


		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, ExportForm.getResolutionAlignmentMap(List.of(COMPLETE,QUARTERS), QUARTER));

		assertThat(contexts).containsExactly (
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 12, 31)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2000, 12, 31)), FeatureGroup.FEATURE, -2, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, 1, eventDate, QUARTERS),
			new DateContext(CDateRange.of(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 12, 31)), FeatureGroup.OUTCOME, 2, eventDate, QUARTERS)
		);
	}

	@Test
	public void rangeRelYearsAlignQuarterNeutralTest() {
		CalendarUnit timeUnit = CalendarUnit.QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 6;
		IndexPlacement indexPlacement = IndexPlacement.NEUTRAL;


		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, ExportForm.getResolutionAlignmentMap(List.of(COMPLETE,YEARS), QUARTER));

		assertThat(contexts).containsExactly (
				new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, null, eventDate, COMPLETE),
				new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2002, 12, 31)), FeatureGroup.OUTCOME, null, eventDate, COMPLETE),
				new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate, YEARS),
				new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2002, 6, 30)), FeatureGroup.OUTCOME, 1, eventDate, YEARS),
				new DateContext(CDateRange.of(LocalDate.of(2002, 7, 1), LocalDate.of(2002, 12, 31)), FeatureGroup.OUTCOME, 2, eventDate, YEARS)
		);
	}

	@Test
	public void rangeRelDaysAlignQuarterNeutralTest() {
		// This should ignore the QUARTER alignment hint because it does not make sense to align a finer resolution than the alignment.

		CalendarUnit timeUnit = CalendarUnit.DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.NEUTRAL;


		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, ExportForm.getResolutionAlignmentMap(List.of(DAYS), QUARTER));

		assertThat(contexts).containsExactly (
				new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 21)), FeatureGroup.FEATURE, -2, eventDate, DAYS),
				new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, -1, eventDate, DAYS),
				new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, 1, eventDate, DAYS),
				new DateContext(CDateRange.of(LocalDate.of(2001, 5, 25), LocalDate.of(2001, 5, 25)), FeatureGroup.OUTCOME, 2, eventDate, DAYS)
		);
	}


	@Test
	public void rangeRelYearsQuarterAlignYearsNeutralTest() {
		// This should ignore the YEAR alignment hint for QUARTERS because the alignment is to coarse. For QUARTERS it should fallback to QUARTER.

		CalendarUnit timeUnit = CalendarUnit.QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 3;
		int outcomeTime = 3;
		IndexPlacement indexPlacement = IndexPlacement.NEUTRAL;


		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, timeUnit, ExportForm.getResolutionAlignmentMap(List.of(YEARS, QUARTERS), YEAR));

		assertThat(contexts).containsExactly (
				new DateContext(CDateRange.of(LocalDate.of(2000, 7, 1), LocalDate.of(2000, 12, 31)), FeatureGroup.FEATURE, -2, eventDate, YEARS),
				new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate, YEARS),
				new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 12, 31)), FeatureGroup.OUTCOME, 1, eventDate, YEARS),
				new DateContext(CDateRange.of(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 3, 31)), FeatureGroup.OUTCOME, 2, eventDate, YEARS),

				new DateContext(CDateRange.of(LocalDate.of(2000, 7, 1), LocalDate.of(2000, 9, 30)), FeatureGroup.FEATURE, -3, eventDate, QUARTERS),
				new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2000, 12, 31)), FeatureGroup.FEATURE, -2, eventDate, QUARTERS),
				new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate, QUARTERS),
				new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, 1, eventDate, QUARTERS),
				new DateContext(CDateRange.of(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 12, 31)), FeatureGroup.OUTCOME, 2, eventDate, QUARTERS),
				new DateContext(CDateRange.of(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 3, 31)), FeatureGroup.OUTCOME, 3, eventDate, QUARTERS)
		);
	}
}
