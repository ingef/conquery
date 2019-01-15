package com.bakdata.conquery.models.concepts.temporal;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;

import java.time.LocalDate;
import java.util.OptionalInt;
import java.util.Random;

public enum TemporalSampler {
	EARLIEST {
		@Override
		public OptionalInt sample(CDateSet data) {
			if (data.isEmpty()) {
				return OptionalInt.empty();
			}

			if (!data.span().hasLowerBound()) {
				return OptionalInt.empty();
			}

			return OptionalInt.of(data.span().getMinValue());
		}
	},
	LATEST {
		@Override
		public OptionalInt sample(CDateSet data) {
			if (data.isEmpty()) {
				return OptionalInt.empty();
			}

			if (!data.span().hasUpperBound()) {
				return OptionalInt.empty();
			}

			return OptionalInt.of(data.span().getMaxValue());
		}
	},
	RANDOM {
		Random random = new Random();

		@Override
		public OptionalInt sample(CDateSet data) {
			if (data.isEmpty()) {
				return OptionalInt.empty();
			}

			CDateRange span = data.span();

			int lower;

			if (span.hasLowerBound()) {
				lower = span.getMinValue();
			}
			else {
				lower = CDate.ofLocalDate(LocalDate.MIN);
			}


			int upper;

			if (span.hasUpperBound()) {
				upper = span.getMaxValue();
			}
			else {
				upper = CDate.ofLocalDate(LocalDate.MAX);
			}


			if (upper == lower) {
				return OptionalInt.of(upper);
			}

			int sample;

			// Sample values as long as they are not inside the data.
			do {
				sample = lower + random.nextInt(upper - lower);
			} while (!data.contains(sample));


			return OptionalInt.of(sample);
		}
	};

	public abstract OptionalInt sample(CDateSet data);

}
