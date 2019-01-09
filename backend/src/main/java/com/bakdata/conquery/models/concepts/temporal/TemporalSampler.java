package com.bakdata.conquery.models.concepts.temporal;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;

import java.time.LocalDate;
import java.util.Random;

public enum TemporalSampler {
	//TODO empty sets
	EARLIEST {
		@Override
		public LocalDate sample(CDateSet data) {
			if (data.isEmpty()) {
				return null;
			}

			if (!data.span().hasLowerBound()) {
				return null;
			}

			return data.span().getMin();
		}
	},
	LATEST {
		@Override
		public LocalDate sample(CDateSet data) {
			if (data.isEmpty()) {
				return null;
			}

			if (!data.span().hasUpperBound()) {
				return null;
			}

			return data.span().getMax();
		}
	},
	RANDOM {
		//TODO How to initalize this with known seed?
		Random random = new Random();

		@Override
		public LocalDate sample(CDateSet data) {
			if (data.isEmpty()) {
				return null;
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
				return CDate.toLocalDate(upper);
			}

			int sample;

			do {
				sample = lower + random.nextInt(upper - lower);
			} while (!data.contains(sample));


			return CDate.toLocalDate(sample);
		}
	};

	public abstract LocalDate sample(CDateSet data);

}
