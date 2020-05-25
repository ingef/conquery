package com.bakdata.conquery.models.common.daterange;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CDateRangeTest {

	@Test
	public void spanClosed() {
		// all null
		assertThat(CDateRange.all().spanClosed(CDateRange.all())).isEqualTo(CDateRange.all());

		// Other null
		assertThat(CDateRange.all().spanClosed(null)).isEqualTo(CDateRange.all());

		// One just exactly
		assertThat(CDateRange.all().spanClosed(CDateRange.exactly(5))).isEqualTo(CDateRange.exactly(5));
		assertThat(CDateRange.exactly(5).spanClosed(CDateRange.all())).isEqualTo(CDateRange.exactly(5));

		// One min only
		assertThat(CDateRange.atLeast(5).spanClosed(CDateRange.all())).isEqualTo(CDateRange.exactly(5));
		assertThat(CDateRange.all().spanClosed(CDateRange.atLeast(5))).isEqualTo(CDateRange.exactly(5));

		// One max only
		assertThat(CDateRange.atMost(5).spanClosed(CDateRange.all())).isEqualTo(CDateRange.exactly(5));
		assertThat(CDateRange.all().spanClosed(CDateRange.atMost(5))).isEqualTo(CDateRange.exactly(5));

		// Opposing min and max
		assertThat(CDateRange.atLeast(5).spanClosed(CDateRange.atMost(5))).isEqualTo(CDateRange.exactly(5));
		assertThat(CDateRange.atMost(5).spanClosed(CDateRange.atLeast(5))).isEqualTo(CDateRange.exactly(5));

		// Only min
		assertThat(CDateRange.atLeast(5).spanClosed(CDateRange.atLeast(0))).isEqualTo(CDateRange.of(0, 5));
		assertThat(CDateRange.atLeast(0).spanClosed(CDateRange.atLeast(5))).isEqualTo(CDateRange.of(0, 5));

		// Only max
		assertThat(CDateRange.atMost(5).spanClosed(CDateRange.atMost(0))).isEqualTo(CDateRange.of(0, 5));
		assertThat(CDateRange.atMost(0).spanClosed(CDateRange.atMost(5))).isEqualTo(CDateRange.of(0, 5));

		// Completely separated
		assertThat(CDateRange.of(0, 5).spanClosed(CDateRange.of(10, 15))).isEqualTo(CDateRange.of(0, 15));
		assertThat(CDateRange.of(10, 15).spanClosed(CDateRange.of(0, 5))).isEqualTo(CDateRange.of(0, 15));

		// Fully enclosed
		assertThat(CDateRange.of(0, 5).spanClosed(CDateRange.of(1, 2))).isEqualTo(CDateRange.of(0, 5));
		assertThat(CDateRange.of(1, 2).spanClosed(CDateRange.of(0, 5))).isEqualTo(CDateRange.of(0, 5));

	}

}