package com.bakdata.conquery.models.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


class RangeTest {

	@Test
	public void exactly() {
		Range<Integer> exactly = Range.exactly(5);

		assertThat((IntPredicate) exactly::contains)
				.accepts(5)
				.rejects(6, 7, 4);

		assertThat((Predicate<Range<Integer>>) exactly::contains)
				.accepts(Range.exactly(5), exactly)
				.rejects(Range.exactly(6), Range.exactly(7), Range.exactly(4));
	}

	@Test
	public void atMost() {
		Range<Integer> range = Range.atMost(5);

		assertThat((IntPredicate) range::contains)
				.accepts(5, 4, CDateRange.MIN_VALUE)
				.rejects(6, CDateRange.MAX_VALUE);

		assertThat((Predicate<Range<Integer>>) range::contains)
				.accepts(Range.exactly(5), Range.atMost(4))
				.rejects(Range.exactly(6), Range.atLeast(5), Range.atLeast(4));
	}

	@Test
	public void atLeast() {
		Range<Integer> range = Range.atLeast(5);

		assertThat((IntPredicate) range::contains)
				.accepts(5, 6, CDateRange.MAX_VALUE)
				.rejects(4, CDateRange.MIN_VALUE)
		;

		assertThat((Predicate<Range<Integer>>) range::contains)
				.rejects(Range.atLeast(4), Range.atMost(4), Range.exactly(4))
				.accepts(Range.exactly(5), Range.exactly(6), Range.atLeast(5), Range.atLeast(6));
	}

	@Test
	public void all() {
		Range<Integer> range = Range.all();

		assertThat((IntPredicate) range::contains)
				.accepts(5, CDateRange.MAX_VALUE, CDateRange.MIN_VALUE);

		assertThat((Predicate<Range<Integer>>) range::contains)
				.accepts(Range.exactly(5), Range.of(5, 10))
				.rejects(Range.atMost(5), Range.atLeast(6));
	}

	@Test
	public void contains() {
		Range<Integer> range = Range.of(5, 10);

		assertThat((IntPredicate) range::contains)
				.accepts(5, 6, 7, 8, 9, 10)
				.rejects(CDateRange.MIN_VALUE, 4, 11, CDateRange.MAX_VALUE);

		assertThat((Predicate<Range<Integer>>) range::contains)
				.accepts(range, Range.of(5, 10), Range.exactly(7), Range.exactly(5), Range.exactly(10), Range.of(6, 9), Range.of(5, 9))
				.rejects(Range.atMost(5), Range.atMost(10), Range.all(), Range.exactly(4), Range.exactly(11), Range.atLeast(5), Range.atLeast(10), Range.of(7, 11), Range.of(11, 12), Range.of(3, 4));
	}

	@Test
	public void invalid() {
		assertThrows(IllegalArgumentException.class, () -> Range.of(5, 4));
	}

	@Test
	public void span() {
		assertThat(Range.exactly(5).span(Range.exactly(6)))
				.isEqualTo(Range.of(5, 6))
				.isEqualTo(Range.exactly(6).span(Range.exactly(5)));

		assertThat(Range.of(5, 7).span(Range.exactly(6)))
				.isEqualTo(Range.of(5, 7));
	}
	
	@Test
	public void coveredYears() {
		CDateRange dateRange = CDateRange.of(LocalDate.of(2000, 9, 2), LocalDate.of(2005, 3, 15));
		
		List<CDateRange> expected = new ArrayList<>();
		expected.add(CDateRange.of(LocalDate.of(2000, 9, 2), LocalDate.of(2000, 12, 31)));
		expected.add(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 12, 31)));
		expected.add(CDateRange.of(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 12, 31)));
		expected.add(CDateRange.of(LocalDate.of(2003, 1, 1), LocalDate.of(2003, 12, 31)));
		expected.add(CDateRange.of(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 12, 31)));
		expected.add(CDateRange.of(LocalDate.of(2005, 1, 1), LocalDate.of(2005, 3, 15)));
		
		assertThat(dateRange.getCoveredYears()).containsExactlyInAnyOrderElementsOf(expected);
	}

	@Test
	public void coveredQuarters() {
		CDateRange dateRange = CDateRange.of(LocalDate.of(2000, 9, 2), LocalDate.of(2002, 3, 15));

		List<CDateRange> expected = new ArrayList<>();
		expected.add(CDateRange.of(LocalDate.of(2000, 9, 2), LocalDate.of(2000, 9, 30)));
		expected.add(CDateRange.of(LocalDate.of(2000, 10, 1), LocalDate.of(2000, 12, 31)));
		expected.add(CDateRange.of(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 3, 31)));
		expected.add(CDateRange.of(LocalDate.of(2001, 4, 1), LocalDate.of(2001, 6, 30)));
		expected.add(CDateRange.of(LocalDate.of(2001, 7, 1), LocalDate.of(2001, 9, 30)));
		expected.add(CDateRange.of(LocalDate.of(2001, 10, 1), LocalDate.of(2001, 12, 31)));
		expected.add(CDateRange.of(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 3, 15)));

		assertThat(dateRange.getCoveredQuarters()).containsExactlyInAnyOrderElementsOf(expected);
	}

	@Test
	public void coveredQuartersNotAFullQuarter() {
		CDateRange dateRange = CDateRange.of(LocalDate.of(2000, 1, 10), LocalDate.of(2000, 3, 15));

		assertThat(dateRange.getCoveredQuarters()).containsExactlyInAnyOrder(CDateRange.of(LocalDate.of(2000, 1, 10), LocalDate.of(2000, 3, 15)));
	}
	
	public static List<Arguments> deserialize() {
		return Arrays.asList(
			Arguments.of(
				"{\"min\":\"2017-01-01\", \"max\":\"2017-01-01\"}", 
				new Range<>(LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 1)), 
				CDateRange.of(LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 1))
			),
			Arguments.of(
				"{\"min\":\"2017-01-01\"}", 
				new Range<>(LocalDate.of(2017, 1, 1), null), 
				CDateRange.atLeast(LocalDate.of(2017, 1, 1))
			)
			,
			Arguments.of(
				"{\"max\":\"2017-01-01\"}", 
				new Range<>(null, LocalDate.of(2017, 1, 1)), 
				CDateRange.atMost(LocalDate.of(2017, 1, 1))
			)
		);
	}
	
	@ParameterizedTest(name="{0}") @MethodSource
	public void deserialize(String json, Range<LocalDate> expected, CDateRange expectedCDateRange) throws IOException {
		ObjectReader reader = Jackson.MAPPER.readerFor(
			Jackson.MAPPER.getTypeFactory().constructParametricType(Range.class, LocalDate.class)
		);
		Range<LocalDate> range = reader.readValue(json);
		assertThat(range).isEqualTo(expected);
		assertThat(range).isEqualToComparingFieldByFieldRecursively(expected);
		
		CDateRange cDateRange = CDateRange.of(range);
		assertThat(cDateRange).isEqualTo(expectedCDateRange);
		assertThat(cDateRange).isEqualToComparingFieldByFieldRecursively(expectedCDateRange);
	}
}