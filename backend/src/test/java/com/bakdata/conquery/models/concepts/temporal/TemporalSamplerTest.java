package com.bakdata.conquery.models.concepts.temporal;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static com.bakdata.conquery.models.common.CDateRange.*;
import static com.bakdata.conquery.models.concepts.temporal.TemporalSampler.*;
import static org.assertj.core.api.Assertions.assertThat;

class TemporalSamplerTest {

	@Test
	public void earliest() {

		assertThat(EARLIEST.sample(generateSet(exactly(LocalDate.of(2011, 1, 10)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));

		assertThat(EARLIEST.sample(generateSet(atLeast(LocalDate.of(2011, 1, 10)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));

		assertThat(EARLIEST.sample(generateSet(atMost(LocalDate.of(2011, 1, 10)))))
				.isNull();

		assertThat(EARLIEST.sample(generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));
	}

	private static CDateSet generateSet(CDateRange... ranges) {
		return CDateSet.create(Arrays.asList(ranges));
	}

	@Test
	public void latest() {

		assertThat(LATEST.sample(generateSet(exactly(LocalDate.of(2011, 1, 10)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));

		assertThat(LATEST.sample(generateSet(atLeast(LocalDate.of(2011, 1, 10)))))
				.isNull();

		assertThat(LATEST.sample(generateSet(atMost(LocalDate.of(2011, 1, 10)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));

		assertThat(LATEST.sample(generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)))))
				.isEqualTo(LocalDate.of(2011, 1, 12));
	}

	@Test
	public void random() {
		assertThat(RANDOM.sample(generateSet(exactly(LocalDate.of(2011, 1, 10)))))
				.isEqualTo(LocalDate.of(2011, 1, 10));

		{
			CDateSet set = generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)));

			assertThat(RANDOM.sample(set))
					.matches(set::contains);
		}

		{
			CDateSet set =
					generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)), of(LocalDate.of(2011, 1, 15), LocalDate.of(2011, 1, 17)));

			assertThat(RANDOM.sample(set))
					.matches(set::contains);
		}

	}

}