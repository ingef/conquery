package com.bakdata.conquery.models.datasets.concepts.temporal;

import static com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSampler.*;

import static com.bakdata.conquery.models.common.CDate.ofLocalDate;
import static com.bakdata.conquery.models.common.daterange.CDateRange.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import org.junit.jupiter.api.Test;

class TemporalSamplerTest {

	private static CDateSet generateSet(CDateRange... ranges) {
		return CDateSet.create(Arrays.asList(ranges));
	}


	@Test
	public void earliest() {

		assertThat(EARLIEST.sample(generateSet(exactly(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		assertThat(EARLIEST.sample(generateSet(atLeast(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		assertThat(EARLIEST.sample(generateSet(atMost(LocalDate.of(2011, 1, 10)))))
				.isEmpty();

		assertThat(EARLIEST.sample(generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));
	}


	@Test
	public void latest() {

		assertThat(LATEST.sample(generateSet(exactly(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		assertThat(LATEST.sample(generateSet(atLeast(LocalDate.of(2011, 1, 10)))))
				.isEmpty();

		assertThat(LATEST.sample(generateSet(atMost(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		assertThat(LATEST.sample(generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 12)));
	}

	@Test
	public void random() {
		assertThat(RANDOM.sample(generateSet(exactly(LocalDate.of(2011, 1, 10)))))
				.hasValue(ofLocalDate(LocalDate.of(2011, 1, 10)));

		{
			CDateSet set = generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)));

			assertThat(RANDOM.sample(set).getAsInt())
					.matches(set::contains);
		}

		{
			CDateSet set =
					generateSet(of(LocalDate.of(2011, 1, 10), LocalDate.of(2011, 1, 12)), of(LocalDate.of(2011, 1, 15), LocalDate.of(2011, 1, 17)));

			assertThat(RANDOM.sample(set).getAsInt())
					.matches(set::contains);
		}

	}

}