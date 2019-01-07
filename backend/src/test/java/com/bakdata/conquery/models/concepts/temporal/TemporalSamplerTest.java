package com.bakdata.conquery.models.concepts.temporal;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static com.bakdata.conquery.models.concepts.temporal.TemporalSampler.*;
import static com.google.common.collect.Range.*;
import static org.assertj.core.api.Assertions.assertThat;

class TemporalSamplerTest {

	@Test
	public void earliest() {

		assertThat(EARLIEST.sample(generateSet(singleton(LocalDate.of(2011, 1, 10)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));

		assertThat(EARLIEST.sample(generateSet(atLeast(LocalDate.of(2011, 1, 10)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));

		assertThat(EARLIEST.sample(generateSet(atMost(LocalDate.of(2011, 1, 10)))))
				.isNull();

		assertThat(EARLIEST.sample(generateSet(open(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));
	}

	private static RangeSet<LocalDate> generateSet(Range<LocalDate>... ranges) {
		return TreeRangeSet.create(Arrays.asList(ranges));
	}

	@Test
	public void latest() {

		assertThat(LATEST.sample(generateSet(singleton(LocalDate.of(2011, 1, 10)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));

		assertThat(LATEST.sample(generateSet(atLeast(LocalDate.of(2011, 1, 10)))))
				.isNull();

		assertThat(LATEST.sample(generateSet(atMost(LocalDate.of(2011, 1, 10)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));

		assertThat(LATEST.sample(generateSet(open(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)))))
				.isEqualTo(LocalDate.of(2011, 1, 12));
	}

	@Test
	public void random() {
		assertThat(RANDOM.sample(generateSet(singleton(LocalDate.of(2011, 1, 10)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));

		{
			RangeSet<LocalDate> set = generateSet(open(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)));

			assertThat(RANDOM.sample(set))
					.matches(set::contains);
		}

		{
			RangeSet<LocalDate> set =
					generateSet(open(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)), open(LocalDate.of(2011, 1, 15), LocalDate.of(2011, 1, 17)));

			assertThat(RANDOM.sample(set))
					.matches(set::contains);
		}

	}

}