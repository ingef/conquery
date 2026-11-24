package com.bakdata.conquery.sql.model.range;

import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertInvalid;
import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertValid;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import com.bakdata.conquery.models.common.InclusiveRange;
import org.junit.jupiter.api.Test;

class DateRangeTest {

	@Test
	void shouldExposeBoundsThroughInclusiveRange() {
		final LocalDate minimum = LocalDate.of(2025, 1, 1);
		final LocalDate maximum = LocalDate.of(2025, 1, 31);
		final InclusiveRange<LocalDate> range = DateRange.closed(minimum, maximum);

		assertEquals(Optional.of(minimum), range.minimum());
		assertEquals(Optional.of(maximum), range.maximum());
	}

	@Test
	void shouldAcceptUnboundedRange() {
		assertValid(new DateRange(Optional.empty(), Optional.empty()));
	}

	@Test
	void shouldRejectInvertedRange() {
		assertInvalid(DateRange.closed(
				LocalDate.of(2025, 2, 1),
				LocalDate.of(2025, 1, 1)
		));
	}
}
