package com.bakdata.conquery.models.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.primitives.Ints;

class CQuarterTest {

	public static List<Arguments> testLastDay() {
		List<Arguments> values = new ArrayList<>();
		IntStream.range(1946, 2100)
		.forEach(year -> {
			for(int quarter=1;quarter<=4;quarter++) {
				LocalDate realFirst = LocalDate.of(year, (quarter-1)*3+1, 1);
				LocalDate realLast = LocalDate.of(year, quarter*3, Month.of(quarter*3).length(false));
				values.add(Arguments.of(realFirst, realLast));
			}
		});
		return values;
	}
	
	@ParameterizedTest(name = "{0}") @MethodSource
	public void testLastDay(LocalDate first, LocalDate last) {
		int firstEpoch = Ints.checkedCast(first.toEpochDay());
		int lastEpoch = Ints.checkedCast(last.toEpochDay());
		assertThat(CQuarter.getLastDay(firstEpoch)).isEqualTo(lastEpoch);
	}
}
