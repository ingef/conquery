package com.bakdata.conquery.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.BitMapCDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BitMapCDateSetTest {

	public static Stream<Arguments> arguments() {
		return Stream
					   .of(
							   Arguments.of(
									   "{1970-01-02/1970-01-02, 1970-04-11/1970-04-12}",
									   new CDateRange[]{
											   CDateRange.of(1, 1),
											   CDateRange.of(100, 101)
									   }
							   ),
							   Arguments.of(
									   "{-∞/1970-01-11, 1970-01-16/1970-01-17}",
									   new CDateRange[]{
											   CDateRange.of(Integer.MIN_VALUE, 10),
											   CDateRange.of(15, 16)
									   }
							   ),
							   Arguments.of(
									   "{2000-01-01/2000-01-01}",
									   new CDateRange[]{
											   CDateRange.of(
													   LocalDate.of(2000, 01, 01),
													   LocalDate.of(2000, 01, 01)
											   )
									   }
							   ),
							   Arguments.of(
									   "{2000-01-01/2004-01-01}",
									   new CDateRange[]{
											   CDateRange.of(
													   LocalDate.of(2000, 01, 01),
													   LocalDate.of(2004, 01, 01)
											   ), CDateRange.of(
											   LocalDate.of(2002, 01, 01),
											   LocalDate.of(2002, 01, 02)
									   )
									   }
							   ),
							   Arguments.of(
									   "{2000-01-01/2000-01-07}",
									   new CDateRange[]{
											   CDateRange.of(
													   LocalDate.of(2000, 01, 01),
													   LocalDate.of(2000, 01, 04)
											   ), CDateRange.of(
											   LocalDate.of(2000, 01, 04),
											   LocalDate.of(2000, 01, 07)
									   )
									   }
							   ),
							   Arguments.of(
									   "{2000-01-01/2000-01-07}",
									   new CDateRange[]{
											   CDateRange.of(
													   LocalDate.of(2000, 01, 01),
													   LocalDate.of(2000, 01, 03)
											   ), CDateRange.of(
											   LocalDate.of(2000, 01, 04),
											   LocalDate.of(2000, 01, 07)
									   )
									   }
							   ),
							   Arguments.of(
									   "{2000-01-01/2000-01-02, 2000-01-04/2000-01-07}",
									   new CDateRange[]{
											   CDateRange.of(
													   LocalDate.of(2000, 01, 01),
													   LocalDate.of(2000, 01, 02)
											   ), CDateRange.of(
											   LocalDate.of(2000, 01, 04),
											   LocalDate.of(2000, 01, 07)
									   )
									   }
							   ),
							   Arguments.of(
									   "{2012-01-01/2012-01-02}",
									   new CDateRange[]{
											   CDateRange.of(
													   LocalDate.of(2012, 01, 02),
													   LocalDate.of(2012, 01, 02)
											   ), CDateRange.of(
											   LocalDate.of(2012, 01, 01),
											   LocalDate.of(2012, 01, 01)
									   )
									   }
							   ),
							   Arguments.of(
									   "{2012-01-02/+∞}",
									   new CDateRange[]{
											   CDateRange.of(
													   LocalDate.of(2012, 01, 02),
													   null
											   )
									   }
							   )
							   , Arguments.of(
									   "{-∞/2012-01-02}",
									   new CDateRange[]{
											   CDateRange.of(null, LocalDate.of(2012, 01, 02))
									   }
							   ),
							   Arguments.of(
									   "{-∞/2012-01-02, 2012-01-04/2012-01-04}",
									   new CDateRange[]{
											   CDateRange.of(null, LocalDate.of(2012, 01, 02)),
											   CDateRange.of(LocalDate.of(2012, 01, 04), LocalDate.of(2012, 01, 04))

									   }
							   ),

							   Arguments.of(
									   "{2011-01-04/2011-01-04, 2012-01-02/+∞}",
									   new CDateRange[]{
											   CDateRange.atLeast(LocalDate.of(2012, 01, 02)),
											   CDateRange.of(LocalDate.of(2011, 01, 04), LocalDate.of(2011, 01, 04))

									   }
							   ),

							   Arguments.of(
									   String.format("{%s}", CDateRange.of(-10, 10)),
									   new CDateRange[]{
											   CDateRange.of(-10, 10)
									   }
							   ),

							   Arguments.of(
									   String.format("{%s}", CDateRange.exactly(-10)),
									   new CDateRange[]{
											   CDateRange.exactly(-10)
									   }
							   ),

							   Arguments.of(
									   String.format("{%s}", CDateRange.of(10, 10)),
									   new CDateRange[]{
											   CDateRange.of(10, 10)
									   }
							   ),
							   Arguments.of(
									   "{1970-01-01/1970-01-01}",
									   new CDateRange[]{
											   CDateRange.of(0, 0)
									   }
							   ),
							   Arguments.of(
									   "{-∞/1970-01-11}",
									   new CDateRange[]{
											   CDateRange.of(-10, 10),
											   CDateRange.of(Integer.MIN_VALUE, 10)
									   }
							   )
							   , Arguments.of(
									   "{-∞/1969-12-31}",
									   new CDateRange[]{
											   CDateRange.atMost(-1)
									   }
							   )
							   , Arguments.of(
									   "{-∞/1970-01-11}",
									   new CDateRange[]{
											   CDateRange.of(9, 10),
											   CDateRange.atMost(5),
											   }
							   )

							   , Arguments.of(
									   "{-∞/1970-01-11}",
									   new CDateRange[]{
											   CDateRange.of(-10, 10),
											   CDateRange.atMost(-5),
											   }
							   )
					   );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("arguments")
	public void testAddMerging(String expected, CDateRange[] ranges) {
		BitMapCDateSet set = BitMapCDateSet.create();
		for (CDateRange range : ranges) {
			set.add(range);
		}
		assertThat(set).hasToString(expected);
	}

	@Test
	public void testRemove() {
		BitMapCDateSet set = BitMapCDateSet.create();
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
	public void testAddMakingAll() {
		assertThat(BitMapCDateSet.create(CDateRange.atMost(1), CDateRange.atLeast(1)).asRanges())
				.containsExactly(CDateRange.all())
		;

		assertThat(BitMapCDateSet.create(CDateRange.atMost(1), CDateRange.atLeast(1)))
				.matches(BitMapCDateSet::isAll)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
		;
	}

	@Test
	public void testRetain() {
		BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(
				LocalDate.of(2000, 01, 01),
				LocalDate.of(2000, 12, 31)
		));

		BitMapCDateSet retain = BitMapCDateSet.create(
				CDateRange.of(
						LocalDate.of(2000, 06, 01),
						LocalDate.of(2000, 06, 20)
				),
				CDateRange.atLeast(LocalDate.of(2000, 12, 01))
		);

		set.retainAll(retain);

		assertThat(set.asRanges()).containsExactly(
				CDateRange.of(
						LocalDate.of(2000, 06, 01),
						LocalDate.of(2000, 06, 20)
				),
				CDateRange.of(
						LocalDate.of(2000, 12, 01),
						LocalDate.of(2000, 12, 31)
				)
		);
	}

	@Test
	public void testRetainRemoveSpanFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
		set.retainAll(CDateRange.of(10, 20));
		assertThat(set.asRanges()).containsExactly(CDateRange.of(10, 20));
	}

	@Test
	public void testRetainRemoveExactlyFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
		set.retainAll(CDateRange.exactly(10));
		assertThat(set.asRanges()).containsExactly(CDateRange.exactly(10));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}

	@Test
	public void testRetainRemoveAtMostFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
		set.retainAll(CDateRange.atMost(10));
		assertThat(set.asRanges()).containsExactly(CDateRange.atMost(10));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}

	@Test
	public void testRetainRemoveAtLeastFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
		set.retainAll(CDateRange.atLeast(10));

		assertThat(set.asRanges()).containsExactly(CDateRange.atLeast(10));
		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;

	}

	@Test
	public void testRetainRemoveNegativeAtLeastFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
		set.retainAll(CDateRange.atLeast(-10));
		assertThat(set.asRanges()).containsExactly(CDateRange.atLeast(-10));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;

	}

	@Test
	public void testRetainRemoveNegativeAtMostFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
		set.retainAll(CDateRange.atMost(-10));
		assertThat(set.asRanges()).containsExactly(CDateRange.atMost(-10));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;

	}

	@Test
	public void removeAtMostFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
		set.remove(CDateRange.atMost(10));

		assertThat(set.asRanges()).containsExactly(CDateRange.atLeast(11));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;

	}

	@Test
	public void removeAtLeastFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
		set.remove(CDateRange.atLeast(10));

		assertThat(set.asRanges()).containsExactly(CDateRange.atMost(9));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;


	}

	@Test
	public void removeExactlyFromItself() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.exactly(10));
		set.remove(CDateRange.exactly(10));

		assertThat(set.asRanges()).isEmpty();

		assertThat(set)
				.matches(BitMapCDateSet::isEmpty)
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;

	}

	@Test
	public void removeExactlyFromAnother() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.exactly(10));
		set.remove(CDateRange.exactly(11));

		assertThat(set.asRanges()).containsExactly(CDateRange.exactly(10));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}


	@Test
	public void removeAllFromFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());

		assertThat(set.asRanges()).containsExactly(CDateRange.all());

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(BitMapCDateSet::isAll)
		;
	}

	@Test
	public void removeExactlyFromRangeOverNegativeAxis() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(-10, 10));
		set.remove(CDateRange.exactly(10));

		assertThat(set.asRanges()).containsExactly(CDateRange.of(-10, 9));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}

	@Test
	public void removeSpanFromRangeOverNegativAxis() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(-10, 10));
		set.remove(CDateRange.of(-1, 1));

		assertThat(set.asRanges()).containsExactly(CDateRange.of(-10, -2), CDateRange.of(2, 10));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}

	@Test
	public void removeNegativeAtMostFromSpanOverNegativeAxis() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(-10, 10));
		set.remove(CDateRange.atMost(-5));

		assertThat(set.asRanges()).containsExactly(CDateRange.of(-4, 10));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}

	@Test
	public void removePositiveAtLeastFromSpanOverNegativeAxis() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(-10, 10));
		set.remove(CDateRange.atLeast(5));

		assertThat(set.asRanges()).containsExactly(CDateRange.of(-10, 4));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}

	@Test
	public void removeAllFromSpanOverNegativeAxis() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(-10, 10));
		set.remove(CDateRange.all());

		assertThat(set.asRanges()).isEmpty();

		assertThat(set)
				.matches(BitMapCDateSet::isEmpty)
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}

	@Test
	public void removeSpanOverNegativeAxisFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
		set.remove(CDateRange.of(-1, 1));

		assertThat(set.asRanges()).containsExactly(CDateRange.atMost(-2), CDateRange.atLeast(2));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}

	@Test
	public void removeExactlyZeroFromAll() {
		final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
		set.remove(CDateRange.exactly(0));

		assertThat(set.asRanges()).containsExactly(CDateRange.atMost(-1), CDateRange.atLeast(1));

		assertThat(set)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}

	@Test
	public void containsExactly() {
		assertThat(BitMapCDateSet.create(CDateRange.of(1, 1)))
				.matches(set -> set.contains(1))
				.matches(set -> !set.contains(-1));

	}

	@Test
	public void containsAtLeas() {
		assertThat(BitMapCDateSet.create(CDateRange.atLeast(1)))
				.matches(set -> set.contains(1))
				.matches(set -> set.contains(2))
				.matches(set -> !set.contains(-1));
	}

	@Test
	public void containsAtMost() {

		assertThat(BitMapCDateSet.create(CDateRange.atMost(1)))
				.matches(set -> set.contains(1))
				.matches(set -> !set.contains(2))
				.matches(set -> set.contains(-1))
				.matches(set -> set.contains(-1000))
		;
	}

	@Test
	public void containsAtLeastAndAtMost() {

		assertThat(BitMapCDateSet.create(CDateRange.atMost(1), CDateRange.atLeast(1)))
				.matches(set -> set.contains(1))
				.matches(set -> set.contains(2))
				.matches(set -> set.contains(-1))
				.matches(set -> set.contains(-1000))
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(BitMapCDateSet::isAll)
		;
	}

	@Test
	public void containsNegativeAtMostAndSpan() {
		assertThat(BitMapCDateSet.create(CDateRange.atMost(-10), CDateRange.of(5, 10)))
				.matches(set -> set.contains(7))
				.matches(set -> set.contains(6))

				.matches(set -> !set.contains(-1))
				.matches(set -> set.contains(-11))
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;
	}

	@Test
	public void containsNegativeAndPositiveSpans() {
		assertThat(BitMapCDateSet.create(CDateRange.of(-10, -5), CDateRange.of(5, 10)))
				.matches(set -> set.contains(7))
				.matches(set -> set.contains(6))

				.matches(set -> set.contains(-7))
				.matches(set -> set.contains(-6))

				.matches(set -> !set.contains(0))
				.matches(set -> !set.contains(100))
				.matches(set -> !set.contains(-100))
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll));
	}

	@Test
	public void intersects() {
		assertThat(BitMapCDateSet.create(CDateRange.of(-10, -5), CDateRange.of(5, 10)))
				.matches(set -> set.intersects(CDateRange.of(-6, 6)))
				.matches(set -> !set.intersects(CDateRange.of(-1, 1)))

				.matches(set -> set.intersects(CDateRange.atLeast(-6)))
				.matches(set -> set.intersects(CDateRange.atMost(-6)))

				.matches(set -> set.intersects(CDateRange.atLeast(6)))
				.matches(set -> set.intersects(CDateRange.atMost(6)))

				.matches(set -> !set.intersects(CDateRange.of(-12, -11)))


				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;


		// Complicated set with overlapping/redundant build instructions
		assertThat(BitMapCDateSet.create(CDateRange.exactly(-7), CDateRange.exactly(7), CDateRange.atMost(-5), CDateRange.atLeast(5)))
				.matches(set -> set.intersects(CDateRange.of(-6, 6)))

				.matches(set -> !set.intersects(CDateRange.of(-1, 1)))


				.matches(set -> set.intersects(CDateRange.atLeast(-6)))
				.matches(set -> set.intersects(CDateRange.atMost(-6)))

				.matches(set -> set.intersects(CDateRange.atLeast(6)))
				.matches(set -> set.intersects(CDateRange.atMost(6)))

				.matches(set -> set.intersects(CDateRange.of(-12, -11)))


				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(Predicate.not(BitMapCDateSet::isAll))
		;


	}

	@Test
	public void testIsAll() {
		assertThat(BitMapCDateSet.createAll())
				.matches(BitMapCDateSet::isAll)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
		;
		assertThat(BitMapCDateSet.create(CDateRange.of(-10, 10), CDateRange.all()))
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(BitMapCDateSet::isAll);

		assertThat(BitMapCDateSet.create(CDateRange.all(), CDateRange.of(-10, 10)))
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.matches(BitMapCDateSet::isAll);

		final BitMapCDateSet out = BitMapCDateSet.create();

		out.add(CDateRange.atLeast(-10));
		out.add(CDateRange.atMost(10));

		assertThat(out)
				.matches(BitMapCDateSet::isAll)
				.matches(Predicate.not(BitMapCDateSet::isEmpty))
				.hasToString("{-∞/+∞}")
		;

	}
}
