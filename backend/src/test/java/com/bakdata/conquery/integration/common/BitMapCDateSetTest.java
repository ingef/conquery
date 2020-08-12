package com.bakdata.conquery.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
									   "{-∞/1969-12-31}",
									   new CDateRange[]{
											   CDateRange.of(9, 10),
											   CDateRange.atMost(5),
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
	public void testRetain() {
		BitMapCDateSet set = BitMapCDateSet.create();
		set.add(CDateRange.of(
				LocalDate.of(2000, 01, 01),
				LocalDate.of(2000, 12, 31)
		));
		BitMapCDateSet retain = BitMapCDateSet.create();
		retain.add(CDateRange.of(
				LocalDate.of(2000, 06, 01),
				LocalDate.of(2000, 06, 20)
		));
		retain.add(CDateRange.atLeast(LocalDate.of(2000, 12, 01)));
		set.retainAll(retain);

		assertThat(set).hasToString("{2000-06-01/2000-06-20, 2000-12-01/2000-12-31}");
	}

	@Test
	public void remove() {
		{
			final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(10, 10));
			set.remove(CDateRange.of(10,10));

			assertThat(set.asRanges()).isEmpty();
		}

		{
			final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());

			assertThat(set.asRanges()).containsExactly(CDateRange.all());
		}

		{
			final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(-10, 10));
			set.remove(CDateRange.of(10,10));

			assertThat(set.asRanges()).containsExactly(CDateRange.of(-10,9));
		}

		{
			final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(-10, 10));
			set.remove(CDateRange.of(-1,1));

			assertThat(set.asRanges()).containsExactly(CDateRange.of(-10,-2), CDateRange.of(2, 10));
		}

		{
			final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(-10, 10));
			set.remove(CDateRange.atMost(-5));

			assertThat(set.asRanges()).containsExactly(CDateRange.of(-4, 10));
		}

		{
			final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(-10, 10));
			set.remove(CDateRange.atLeast(5));

			assertThat(set.asRanges()).containsExactly(CDateRange.of(-10, 4));
		}

		{
			final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.of(-10, 10));
			set.remove(CDateRange.all());

			assertThat(set.asRanges()).isEmpty();
		}

		{
			final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
			set.remove(CDateRange.of(-1,1));

			assertThat(set.asRanges()).containsExactly(CDateRange.atMost(-2), CDateRange.atLeast(2));
		}

		{
			final BitMapCDateSet set = BitMapCDateSet.create(CDateRange.all());
			set.remove(CDateRange.exactly(0));

			assertThat(set.asRanges()).containsExactly(CDateRange.atMost(-1), CDateRange.atLeast(1));
		}
	}


}
