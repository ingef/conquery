package com.bakdata.conquery.models.forms;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.NavigableMap;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.integration.common.CDateSetTest;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;

public class DateContextTest {

	@Test
	public void rangeCompleteTest() {
		CDateRange mask = new CDateRange(LocalDate.of(2001, 5, 23), LocalDate.of(2005, 4, 21));
		CDateSet dateSet = CDateSet.create(new CDateRange(LocalDate.of(2000, 12, 1),LocalDate.of(2003, 1, 1)));
		List<DateContext> contexts = DateContext.generateAbsoluteContexts(dateSet, mask, DateContextMode.COMPLETE);
		
		List<DateContext> expected = new ArrayList<>();
		DateContext dc = new DateContext(mask);
		dc.setFeatureGroups(EnumSet.of(FeatureGroup.OUTCOME));
		expected.add(dc);
		assertThat(contexts).isEqualTo(expected);
	}
}
