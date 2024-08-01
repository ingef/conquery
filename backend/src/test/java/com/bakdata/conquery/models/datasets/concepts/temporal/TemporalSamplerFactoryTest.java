package com.bakdata.conquery.models.datasets.concepts.temporal;

import static com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSamplerFactory.*;
import static com.bakdata.conquery.models.common.CDate.ofLocalDate;
import static com.bakdata.conquery.models.common.daterange.CDateRange.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import org.junit.jupiter.api.Test;

class TemporalSamplerFactoryTest {

	@Test
	public void earliest() {

		final Sampler sampler = EARLIEST.sampler(0);
		assertThat(sampler.sample(generateSet(exactly(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		assertThat(sampler.sample(generateSet(atLeast(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		assertThat(sampler.sample(generateSet(atMost(LocalDate.of(2011, 1, 10)))))
				.isEmpty();

		assertThat(sampler.sample(generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));
	}

	private static CDateSet generateSet(CDateRange... ranges) {
		return CDateSet.create(Arrays.asList(ranges));
	}

	@Test
	public void latest() {

		final Sampler sampler = LATEST.sampler(0);
		assertThat(sampler.sample(generateSet(exactly(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		assertThat(sampler.sample(generateSet(atLeast(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		assertThat(sampler.sample(generateSet(atMost(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		assertThat(sampler.sample(generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 12)));
	}

	@Test
	public void random() {
		assertThat(RANDOM.sampler(0).sample(generateSet(exactly(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		{
			assertThat(RANDOM.sampler(0).sample(CDateSet.createEmpty())).isEmpty();
		}

		{
			final CDateSet set = generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)));
			final Sampler sampler = RANDOM.sampler(0);

			for (int rep = 0; rep < 10; rep++) {
				assertThat(sampler.sample(set).getAsInt())
						.matches(set::contains);

			}
		}


		{
			final CDateSet set =
					generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)), of(LocalDate.of(2011, 1, 15), LocalDate.of(2011, 1, 17)));

			final Sampler sampler = RANDOM.sampler(0);
			for (int rep = 0; rep < 10; rep++) {

				assertThat(sampler.sample(set).getAsInt())
						.matches(set::contains);
			}
		}

	}

}