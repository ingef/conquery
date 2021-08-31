package com.bakdata.conquery.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CDateSetTest {

	private static ConqueryConfig config = new ConqueryConfig();

	public static Stream<Arguments> arguments() {
		return Stream
			.of(
				Arguments.of("{2000-01-01/2000-01-01}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 1, 1),
						LocalDate.of(2000, 1, 1)
					)}
				),
				Arguments.of("{2000-01-01/2000-01-01, 2001-01-01/2001-01-02}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 1, 1),
						LocalDate.of(2000, 1, 1)
					), CDateRange.of(
						LocalDate.of(2001, 1, 1),
						LocalDate.of(2001, 1, 2)
					)}
				),
				Arguments.of("{2000-01-01/2004-01-01}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 1, 1),
						LocalDate.of(2004, 1, 1)
					), CDateRange.of(
						LocalDate.of(2002, 1, 1),
						LocalDate.of(2002, 1, 2)
					)}
				),
				Arguments.of("{2000-01-01/2000-01-07}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 1, 1),
						LocalDate.of(2000, 1, 4)
					), CDateRange.of(
						LocalDate.of(2000, 1, 4),
						LocalDate.of(2000, 1, 7)
					)}
				),
				Arguments.of("{2000-01-01/2000-01-07}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 1, 1),
						LocalDate.of(2000, 1, 3)
					), CDateRange.of(
						LocalDate.of(2000, 1, 4),
						LocalDate.of(2000, 1, 7)
					)}
				),
				Arguments.of("{2000-01-01/2000-01-02, 2000-01-04/2000-01-07}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2000, 1, 1),
						LocalDate.of(2000, 1, 2)
					), CDateRange.of(
						LocalDate.of(2000, 1, 4),
						LocalDate.of(2000, 1, 7)
					)}
				),
				Arguments.of("{2012-01-01/2012-01-02}",
					new CDateRange[] {CDateRange.of(
						LocalDate.of(2012, 1, 2),
						LocalDate.of(2012, 1, 2)
					), CDateRange.of(
						LocalDate.of(2012, 1, 1),
						LocalDate.of(2012, 1, 1)
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
			LocalDate.of(2000, 1, 1),
			LocalDate.of(2000, 12, 31)
		));
		set.remove(CDateRange.of(
			LocalDate.of(2000, 6, 1),
			LocalDate.of(2000, 6, 20)
		));
		assertThat(set).hasToString("{2000-01-01/2000-05-31, 2000-06-21/2000-12-31}");
	}
	
	@Test
	public void testRetain() {
		CDateSet set = CDateSet.create();
		set.add(CDateRange.of(
			LocalDate.of(2000, 1, 1),
			LocalDate.of(2000, 12, 31)
		));
		CDateSet retain = CDateSet.create();
		retain.add(CDateRange.of(
			LocalDate.of(2000, 6, 1),
			LocalDate.of(2000, 6, 20)
		));
		retain.add(CDateRange.atLeast(LocalDate.of(2000, 12, 1)));
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
	public void testMaskedTEST() {
		CDateSet set = CDateSet.create();
		CDateSet mask = CDateSet.create(CDateRange.of(-10, 10));

		set.maskedAdd(CDateRange.of(-5, 5), mask);

		assertThat(set.asRanges()).containsExactly(CDateRange.of(-5, 5));
	}

	@Test
	public void testMaskedAddClosedMaskAtMost() {
		CDateSet set = CDateSet.create();

		CDateSet mask1 = CDateSet.create(CDateRange.of(10, 19));

		CDateSet mask2 = CDateSet.create(CDateRange.of(20, 30));

		set.maskedAdd(CDateRange.of(0, 5), mask1);
		set.maskedAdd(CDateRange.of(35, 36), mask2);

		assertThat(set.asRanges()).isEmpty();
	}

	@Test
	public void testMaskedAddNoIntersection() {
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

	public static Stream<Arguments> argumentsParsing() {
		return Stream
				.of(
						Arguments.of(
								"{2000-01-01/2000-01-01}",
								CDateSet.create(CDateRange.of(LocalDate.of(2000,01,01), LocalDate.of(2000,01,01)))
						),
						Arguments.of(
								"01.01.2000-01.01.2000",
								CDateSet.create(CDateRange.of(LocalDate.of(2000,01,01), LocalDate.of(2000,01,01)))
						),
						Arguments.of(
								"{2000-01-01/2000-01-01, 2001-01-01/2001-01-01}",
								CDateSet.create(
										List.of(
												CDateRange.of(LocalDate.of(2000,01,01), LocalDate.of(2000,01,01)),
												CDateRange.of(LocalDate.of(2001,01,01), LocalDate.of(2001,01,01)))
								)
						),
						Arguments.of(
								"01.01.2000-01.01.2000, 01.01.2001-01.01.2001",
								CDateSet.create(
										List.of(
										CDateRange.of(LocalDate.of(2000,01,01), LocalDate.of(2000,01,01)),
										CDateRange.of(LocalDate.of(2001,01,01), LocalDate.of(2001,01,01)))
								)
						),
						Arguments.of(
								"{2000-05-01, 2000-01-01/2000-01-01, 2001-01-01/2001-01-01}",
								CDateSet.create(
										List.of(
												CDateRange.exactly(LocalDate.of(2000,05,01)),
												CDateRange.of(LocalDate.of(2000,01,01), LocalDate.of(2000,01,01)),
												CDateRange.of(LocalDate.of(2001,01,01), LocalDate.of(2001,01,01)))
								)
						),
						Arguments.of(
								"01.05.2000, 01.01.2000-01.01.2000, 01.01.2001-01.01.2001",
								CDateSet.create(
										List.of(
												CDateRange.exactly(LocalDate.of(2000,05,01)),
												CDateRange.of(LocalDate.of(2000,01,01), LocalDate.of(2000,01,01)),
												CDateRange.of(LocalDate.of(2001,01,01), LocalDate.of(2001,01,01)))
								)
						)
				);
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("argumentsParsing")
	public void parse(String input, CDateSet expected) {
		CDateSet set = config.getLocale().getDateReader().parseToCDateSet(input);
		assertThat(set).isEqualTo(expected);
	}

}
