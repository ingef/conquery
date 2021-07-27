package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
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
			out.add(CDateRange.atMost(dateReader.parseToLocalDate(value)));
		}
	},
	START_DATE {
		@Override
		public void readDates(String value, DateReader dateReader, CDateSet out) {
			out.add(CDateRange.atLeast(dateReader.parseToLocalDate(value)));
		}
	},
	DATE_RANGE {
		@Override
		public void readDates(String value, DateReader dateReader, CDateSet out) {
			out.add(DateRangeParser.parseISORange(value, dateReader));
		}
	},
	DATE_SET {
		@Override
		public void readDates(String value, DateReader dateReader, CDateSet out) {

			out.addAll(CDateSet.parse(value, dateReader));
		}
	},
	ALL {
		@Override
		public void readDates(String value, DateReader dateReader, CDateSet out) {
			out.add(CDateRange.all());
		}
	};

	public abstract void readDates(String value, DateReader dateReader, CDateSet out);

	public static int[] select(List<DateFormat> dateFormat) {
		final List<DateFormat> distinct = dateFormat.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());

		// => ALL
		if (distinct.isEmpty()) {
			return new int[0];
		}


		if (distinct.size() == 1) {
			return new int[]{dateFormat.indexOf(distinct.get(0))};
		}

		if (distinct.size() == 2 && distinct.get(0).equals(START_DATE) && distinct.get(1).equals(END_DATE)) {
			return new int[]{dateFormat.indexOf(START_DATE), dateFormat.indexOf(END_DATE)};
		}

		throw new IllegalStateException("can only handle 1 or 2 format columns"); //TODO map error

	}
}
