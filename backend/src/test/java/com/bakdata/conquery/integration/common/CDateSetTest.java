package com.bakdata.conquery.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CDateSetTest {

	public static Stream<Arguments> arguments() {
		return Stream
			.of(
				Arguments.of("{2000-01-01/2000-01-01}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 01, 01),
						LocalDate.of(2000, 01, 01)
					)}
				),
				Arguments.of("{2000-01-01/2000-01-01, 2001-01-01/2001-01-02}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 01, 01),
						LocalDate.of(2000, 01, 01)
					), CDateRange.of(
						LocalDate.of(2001, 01, 01),
						LocalDate.of(2001, 01, 02)
					)}
				),
				Arguments.of("{2000-01-01/2004-01-01}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 01, 01),
						LocalDate.of(2004, 01, 01)
					), CDateRange.of(
						LocalDate.of(2002, 01, 01),
						LocalDate.of(2002, 01, 02)
					)}
				),
				Arguments.of("{2000-01-01/2000-01-07}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 01, 01),
						LocalDate.of(2000, 01, 04)
					), CDateRange.of(
						LocalDate.of(2000, 01, 04),
						LocalDate.of(2000, 01, 07)
					)}
				),
				Arguments.of("{2000-01-01/2000-01-07}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 01, 01),
						LocalDate.of(2000, 01, 03)
					), CDateRange.of(
						LocalDate.of(2000, 01, 04),
						LocalDate.of(2000, 01, 07)
					)}
				),
				Arguments.of("{2000-01-01/2000-01-02, 2000-01-04/2000-01-07}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 01, 01),
						LocalDate.of(2000, 01, 02)
					), CDateRange.of(
						LocalDate.of(2000, 01, 04),
						LocalDate.of(2000, 01, 07)
					)}
				),
				Arguments.of("{2012-01-01/2012-01-02}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2012, 01, 02),
						LocalDate.of(2012, 01, 02)
					), CDateRange.of(
						LocalDate.of(2012, 01, 01),
						LocalDate.of(2012, 01, 01)
					)}
				)
			);
	}
	
	@ParameterizedTest(name="{0}") @MethodSource("arguments")
	public void testAddMerging(String expected, CDateRange[] ranges) {
		CDateSet set = CDateSet.create();
		for(CDateRange range: ranges) {
			set.add(range);
		}
		assertThat(set).hasToString(expected);
	}
	
	@Test
	public void testRemove() {
		CDateSet set = CDateSet.create();
		set.add(CDateRange.of(
			LocalDate.of(2000, 01, 01),
			LocalDate.of(2000, 12, 31)
		));
		set.remove(CDateRange.of(
			LocalDate.of(2000, 06, 01),
			LocalDate.of(2000, 06, 20)
		));
		assertThat(set).hasToString("{2000-01-01/2000-05-31, 2000-06-21/2000-12-31}");
	}
	
	@Test
	public void testRetain() {
		CDateSet set = CDateSet.create();
		set.add(CDateRange.of(
			LocalDate.of(2000, 01, 01),
			LocalDate.of(2000, 12, 31)
		));
		CDateSet retain = CDateSet.create();
		retain.add(CDateRange.of(
			LocalDate.of(2000, 06, 01),
			LocalDate.of(2000, 06, 20)
		));
		retain.add(CDateRange.atLeast(LocalDate.of(2000, 12, 01)));
		set.retainAll(retain);
		assertThat(set).hasToString("{2000-06-01/2000-06-20, 2000-12-01/2000-12-31}");
	}

	@Test
	public void testMaskedAddClosedMaskClosed() {
		CDateSet set = CDateSet.create();
		CDateSet mask = CDateSet.create(CDateRange.of(-10, 10));

		set.maskedAdd(CDateRange.of(-5, 5), mask);

		assertThat(set.asRanges()).containsExactly(CDateRange.of(-5, 5));
	}

	@Test
	public void testMaskedAddClosedMaskAtMost() {
		CDateSet set = CDateSet.create();
		CDateSet mask = CDateSet.create(CDateRange.atMost(4));

		set.maskedAdd(CDateRange.of(-5, 5), mask);

		assertThat(set.asRanges()).containsExactly(CDateRange.of(-5, 4));
	}

	@Test
	public void testMaskedAddClosedMaskAtLeast() {
		CDateSet set = CDateSet.create();
		CDateSet mask = CDateSet.create(CDateRange.atLeast(4));

		set.maskedAdd(CDateRange.of(-5, 5), mask);

		assertThat(set.asRanges()).containsExactly(CDateRange.of(4, 5));
	}

	@Test
	public void testMaskedAddAtMostMaskClosed() {
		CDateSet set = CDateSet.create();
		CDateSet mask = CDateSet.create(CDateRange.of(-10,10));

		set.maskedAdd(CDateRange.atMost(5), mask);

		assertThat(set.asRanges()).containsExactly(CDateRange.of(-10, 5));
	}

	@Test
	public void testMaskedAddAtLeastMaskClosed() {
		CDateSet set = CDateSet.create();
		CDateSet mask = CDateSet.create(CDateRange.of(-10,10));

		set.maskedAdd(CDateRange.atLeast(5), mask);

		assertThat(set.asRanges()).containsExactly(CDateRange.of(5, 10));
	}

	@Test
	public void testMaskedAddAtLeastMaskMultiple() {
		CDateSet set = CDateSet.create();
		CDateSet mask = CDateSet.create(List.of(CDateRange.of(-10, -5), CDateRange.of(1, 10), CDateRange.atLeast(30)));

		set.maskedAdd(CDateRange.atLeast(5), mask);

		assertThat(set.asRanges()).containsExactly(CDateRange.of(5, 10), CDateRange.atLeast(30));
	}

}
