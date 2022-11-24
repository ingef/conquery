package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import java.util.OptionalInt;
import java.util.Random;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;

/**
 * A class implementing several sampling schemes for {@link CDateSet}.
 */
public enum TemporalSamplerFactory {
	/**
	 * Sampler that returns the earliest date of the {@link CDateSet}, if present, or empty if the Set has no lowerbound.
	 */
	EARLIEST {
		@Override
		public Sampler sampler(int today) {
			return data -> {
				if (data.isEmpty()) {
					return OptionalInt.empty();
				}

				final CDateRange span = data.span();

				if (span.hasLowerBound()) {
					return OptionalInt.of(span.getMinValue());
				}

				return OptionalInt.empty();
			};
		}
	},
	/**
	 * Sampler that returns the latest date of the {@link CDateSet}, if present, or empty if the Set has no upperbound.
	 */
	LATEST {
		@Override
		public Sampler sampler(int today) {
			return data -> {
				if (data.isEmpty()) {
					return OptionalInt.empty();
				}

				final CDateRange span = data.span();

				if (span.hasUpperBound()) {
					return OptionalInt.of(span.getMaxValue());
				}

				if(span.hasLowerBound() && span.getMinValue() > today){
					return OptionalInt.of(span.getMinValue());
				}

				return OptionalInt.of(today);

			};
		}
	},
	/**
	 * Creates a sampler that returns a random date that is inside {@link CDateSet}.
	 */
	RANDOM {
		@Override
		public Sampler sampler(int today) {
			final Random random = new Random(ConqueryConstants.RANDOM_SEED);

			return data -> {
				if (data.isEmpty()) {
					return OptionalInt.empty();
				}

				final CDateRange span = data.span();

				// It would not produce sensible to just return random data.
				if (span.isAll()) {
					return OptionalInt.empty();
				}

				final int lower;

				if (span.hasLowerBound()) {
					lower = span.getMinValue();
				}
				else {
					// Very rare edge case with no real answer to give
					lower = CDateRange.NEGATIVE_INFINITY;
				}


				final int upper;

				if (span.hasUpperBound()) {
					upper = span.getMaxValue();
				}
				else {
					// Since there is no satisfying real value, but real values are preferred, we set the maximum to today for sampling purposes.
					upper = today;
				}


				if (upper == lower) {
					return OptionalInt.of(upper);
				}

				int sample;

				// Sample new values as long as they are not inside the data.
				do {
					sample = lower + random.nextInt(upper - lower);
				} while (!data.contains(sample));


				return OptionalInt.of(sample);
			};
		}
	};

	/**
	 * Get a date from within the {@link CDateSet} that is produced according to a sampling scheme.
	 *
	 * @return A sampler returning a date fitting the sampling criteria. Or {@link OptionalInt#empty()} if none is found.
	 */
	public abstract Sampler sampler(int today);

	@FunctionalInterface
	public interface Sampler {
		OptionalInt sample(CDateSet data);
	}

}
