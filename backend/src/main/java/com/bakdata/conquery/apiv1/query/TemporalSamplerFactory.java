package com.bakdata.conquery.apiv1.query;

import java.util.OptionalInt;
import java.util.Random;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.IndexSelector;
import lombok.experimental.UtilityClass;

/**
 * Creates temporal samplers for the shared form index selector values.
 */
@UtilityClass
public class TemporalSamplerFactory {

	public Sampler sampler(IndexSelector selector, int today) {
		return switch (selector) {
			case EARLIEST -> data -> {
				if (data.isEmpty()) {
					return OptionalInt.empty();
				}

				final CDateRange span = data.span();
				if (span.hasLowerBound()) {
					return OptionalInt.of(span.getMinValue());
				}

				return OptionalInt.empty();
			};
			case LATEST -> data -> {
				if (data.isEmpty()) {
					return OptionalInt.empty();
				}

				final CDateRange span = data.span();
				if (span.hasUpperBound()) {
					return OptionalInt.of(span.getMaxValue());
				}

				if (span.hasLowerBound() && span.getMinValue() > today) {
					return OptionalInt.of(span.getMinValue());
				}

				return OptionalInt.of(today);
			};
			case RANDOM -> {
				final Random random = new Random(ConqueryConstants.RANDOM_SEED);
				yield data -> {
					if (data.isEmpty()) {
						return OptionalInt.empty();
					}

					final CDateRange span = data.span();
					if (span.isAll()) {
						return OptionalInt.empty();
					}

					final int lower;
					if (span.hasLowerBound()) {
						lower = span.getMinValue();
					}
					else {
						lower = CDateRange.NEGATIVE_INFINITY;
					}

					final int upper;
					if (span.hasUpperBound()) {
						upper = span.getMaxValue();
					}
					else {
						upper = today;
					}

					if (upper == lower) {
						return OptionalInt.of(upper);
					}

					int sample;
					do {
						sample = lower + random.nextInt(upper - lower);
					} while (!data.contains(sample));

					return OptionalInt.of(sample);
				};
			}
		};
	}

	@FunctionalInterface
	public interface Sampler {
		OptionalInt sample(CDateSet data);
	}
}
