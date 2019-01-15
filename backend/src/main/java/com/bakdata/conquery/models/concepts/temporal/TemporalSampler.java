package com.bakdata.conquery.models.concepts.temporal;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;

import java.util.OptionalInt;
import java.util.Random;

public enum TemporalSampler {
	//TODO empty sets
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
		//TODO How to initialize this with known seed?
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
				lower = 0;
			}


			int upper;

			if (span.hasUpperBound()) {
				upper = span.getMaxValue();
			}
			else {
				upper = Integer.MAX_VALUE; //TODO wat do?
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
