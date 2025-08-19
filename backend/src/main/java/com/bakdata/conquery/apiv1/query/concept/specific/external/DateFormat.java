package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.util.DateReader;

public enum DateFormat {
	EVENT_DATE {
		@Override
		public void readDates(String value, DateReader dateReader, CDateSet out) {
			out.add(CDateRange.exactly(dateReader.parseToLocalDate(value)));
		}
	},
	END_DATE {
		@Override
		public void readDates(String value, DateReader dateReader, CDateSet out) {
			final CDateRange parsed = CDateRange.atMost(dateReader.parseToLocalDate(value));

			if(out.isEmpty()){
				out.add(parsed);
				return;
			}

			final CDateRange span = out.span();

			out.clear();

			out.add(span.spanClosed(parsed));
		}
	},
	START_DATE {
		@Override
		public void readDates(String value, DateReader dateReader, CDateSet out) {
			final CDateRange parsed = CDateRange.atLeast(dateReader.parseToLocalDate(value));

			if(out.isEmpty()){
				out.add(parsed);
				return;
			}

			final CDateRange span = out.span();

			out.clear();

			out.add(span.spanClosed(parsed));
		}
	},
	DATE_RANGE {
		@Override
		public void readDates(String value, DateReader dateReader, CDateSet out) {
			out.add(dateReader.parseToCDateRange(value));
		}
	},
	DATE_SET {
		@Override
		public void readDates(String value, DateReader dateReader, CDateSet out) {
			CDateSet parsed = dateReader.parseToCDateSet(value);
			if (parsed == null ) {
				return;
			}
			out.addAll(parsed);
		}
	},
	ALL {
		@Override
		public void readDates(String value, DateReader dateReader, CDateSet out) {
			out.add(CDateRange.all());
		}
	};

	/**
	 * Try and parse value using dateReader, adding result into out set.
	 */
	public abstract void readDates(String value, DateReader dateReader, CDateSet out);

	public static final Set<String> NAMES = Arrays.stream(DateFormat.values()).map(Enum::name).collect(Collectors.toSet());

}
