package com.bakdata.conquery.models.query.queryplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.DateAggregationAction;
import org.junit.jupiter.api.Test;

class DateAggregatorTest {

	@Test
	void shouldBlockDates() {
		assertNull(DateAggregator.aggregate(DateAggregationAction.BLOCK, Set.of(dateSet(1, 2))));
	}

	@Test
	void shouldMergeDates() {
		CDateSet result = DateAggregator.aggregate(
				DateAggregationAction.MERGE,
				Set.of(dateSet(1, 3), dateSet(5, 7))
		);

		assertEquals(CDateSet.create(Set.of(CDateRange.of(1, 3), CDateRange.of(5, 7))), result);
	}

	@Test
	void shouldIntersectDates() {
		CDateSet result = DateAggregator.aggregate(
				DateAggregationAction.INTERSECT,
				Set.of(dateSet(1, 5), dateSet(3, 7))
		);

		assertEquals(dateSet(3, 5), result);
	}

	@Test
	void shouldNegateDates() {
		CDateSet result = DateAggregator.aggregate(DateAggregationAction.NEGATE, Set.of(dateSet(3, 5)));

		assertTrue(result.contains(2));
		assertFalse(result.contains(3));
		assertFalse(result.contains(5));
		assertTrue(result.contains(6));
	}

	private static CDateSet dateSet(int minimum, int maximum) {
		return CDateSet.create(CDateRange.of(minimum, maximum));
	}
}
