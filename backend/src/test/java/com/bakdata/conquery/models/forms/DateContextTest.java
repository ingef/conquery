package com.bakdata.conquery.models.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.TimeUnit;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import org.junit.jupiter.api.Test;

public class DateContextTest {

	@Test
	public void rangeAbsCompleteTest() {
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.COMPLETE_ONLY);

		DateContext dc = new DateContext(mask);
		dc.setFeatureGroup(FeatureGroup.OUTCOME);
		assertThat(contexts).isEqualTo(Arrays.asList(dc));
	}

	@Test
	public void rangeAbsYearTest() {
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.YEAR_WISE);

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
	public void rangeAbsQuarterTest() {
		CDateRange mask = CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2003, 4, 21));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(mask, DateContextMode.QUARTER_WISE);

		assertThat(contexts).extracting(DateContext::getDateRange).containsExactlyInAnyOrder (
			mask,
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
		TimeUnit resolution = TimeUnit.DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 1;
		IndexPlacement indexPlacement = IndexPlacement.BEFORE;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, true, resolution);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 23)), FeatureGroup.FEATURE, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, -1, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 5, 23)), FeatureGroup.FEATURE, 0, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, 1, eventDate)
		);
	}
	
	@Test
	public void rangeRelDaysBeforeCompleteOnlyTest() {
		TimeUnit resolution = TimeUnit.DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 1;
		IndexPlacement indexPlacement = IndexPlacement.BEFORE;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, false, resolution);

		List<DateContext> expectedRanges = Arrays.asList(
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 23)), FeatureGroup.FEATURE, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, null, eventDate)
		);
		assertThat(contexts).containsExactlyInAnyOrderElementsOf(expectedRanges);
	}
	
	@Test
	public void rangeRelDaysAfterTest() {
		TimeUnit resolution = TimeUnit.DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.AFTER;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, true, resolution);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 21)), FeatureGroup.FEATURE, -2, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, -1, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 23), LocalDate.of(2001, 5, 23)), FeatureGroup.OUTCOME, 0, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, 1, eventDate)
		);
	}
	
	@Test
	public void rangeRelDaysNeutralTest() {
		TimeUnit resolution = TimeUnit.DAYS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.NEUTRAL;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, true, resolution);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 21), LocalDate.of(2001, 5, 21)), FeatureGroup.FEATURE, -2, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 22), LocalDate.of(2001, 5, 22)), FeatureGroup.FEATURE, -1, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 25)), FeatureGroup.OUTCOME, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 24), LocalDate.of(2001, 5, 24)), FeatureGroup.OUTCOME, 1, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 5, 25), LocalDate.of(2001, 5, 25)), FeatureGroup.OUTCOME, 2, eventDate)
		);
	}
	
	@Test
	public void rangeRelQuarterBeforeTest() {
		TimeUnit resolution = TimeUnit.QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 1;
		IndexPlacement indexPlacement = IndexPlacement.BEFORE;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, true, resolution);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 6, 30)), FeatureGroup.FEATURE, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 4, 1), LocalDate.of(2001, 6, 30)), FeatureGroup.FEATURE, 0, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, 1, eventDate)
		);
	}
	
	@Test
	public void rangeRelQuarterAfterTest() {
		TimeUnit resolution = TimeUnit.QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.AFTER;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, true, resolution);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2000, 12, 31)), FeatureGroup.FEATURE, -2, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 4, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 4, 1), LocalDate.of(2001, 6, 30)), FeatureGroup.OUTCOME, 0, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, 1, eventDate)
		);
	}
	
	@Test
	public void rangeRelQuarterNeutralTest() {
		TimeUnit resolution = TimeUnit.QUARTERS;
		LocalDate eventDate = LocalDate.of(2001, 5, 23);
		int event = CDate.ofLocalDate(eventDate);
		int featureTime = 2;
		int outcomeTime = 2;
		IndexPlacement indexPlacement = IndexPlacement.NEUTRAL;
		

		List<DateContext> contexts = DateContext.generateRelativeContexts(event, indexPlacement, featureTime, outcomeTime, true, resolution);

		assertThat(contexts).containsExactlyInAnyOrder (
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2000, 12, 31)), FeatureGroup.FEATURE, -2, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)), FeatureGroup.FEATURE, -1, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 12, 31)), FeatureGroup.OUTCOME, null, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)), FeatureGroup.OUTCOME, 1, eventDate),
			new DateContext(CDateRange.of(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 12, 31)), FeatureGroup.OUTCOME, 2, eventDate)
		);
	}
}
