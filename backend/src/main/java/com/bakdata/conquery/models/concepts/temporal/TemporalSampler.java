package com.bakdata.conquery.models.concepts.temporal;

import com.bakdata.conquery.models.common.CDate;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;

import java.time.LocalDate;
import java.util.Random;

public enum TemporalSampler {
	//TODO empty sets
	EARLIEST {
		@Override
		public LocalDate sample(RangeSet<LocalDate> data) {
			if (data.isEmpty()) {
				return null;
			}

			if (!data.span().hasLowerBound()) {
				return null;
			}

			return data.span().lowerEndpoint();
		}
	},
	LATEST {
		@Override
		public LocalDate sample(RangeSet<LocalDate> data) {
			if (data.isEmpty()) {
				return null;
			}

			if (!data.span().hasUpperBound()) {
				return null;
			}

			return data.span().upperEndpoint();
		}
	},
	RANDOM {
		//TODO How to initalize this with known seed?
		Random random = new Random();

		@Override
		public LocalDate sample(RangeSet<LocalDate> data) {
			if (data.isEmpty()) {
				return null;
			}


			Range<LocalDate> span = data.span();

			int lower;

			if (span.hasLowerBound()) {
				lower = CDate.ofLocalDate(span.lowerEndpoint());
			}
			else {
				lower = CDate.ofLocalDate(LocalDate.ofEpochDay(0));
			}


			int upper;

			if (span.hasUpperBound()) {
				upper = CDate.ofLocalDate(span.upperEndpoint());
			}
			else {
				upper = CDate.ofLocalDate(LocalDate.ofEpochDay(Long.MAX_VALUE)); //TODO wat do?
			}


			if (upper == lower) {
				return CDate.toLocalDate(upper);
			}

			int sample;

			do {
				sample = lower + random.nextInt(upper - lower);
			} while (!data.contains(CDate.toLocalDate(sample)));


			return CDate.toLocalDate(sample);
		}
	};

	public abstract LocalDate sample(RangeSet<LocalDate> data);

}
