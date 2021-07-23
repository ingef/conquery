package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.time.LocalDate;
import java.util.Collections;
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
		public CDateSet readDates(int[] formats, String[] row, DateReader dateReader) {
			final int index = formats[0];

			return CDateSet.create(Collections.singleton(CDateRange.exactly(dateReader.parseToLocalDate(row[index]))));
		}
	},
	END_DATE {
		@Override
		public CDateSet readDates(int[] formats, String[] row, DateReader dateReader) {

			if (formats.length == 1) {
				return CDateSet.create(CDateRange.atMost(dateReader.parseToLocalDate(row[0])));
			}

			return END_DATE.readDates(formats, row, dateReader);
		}
	},
	START_DATE {
		@Override
		public CDateSet readDates(int[] formats, String[] row, DateReader dateReader) {

			if (formats.length == 1) {
				return CDateSet.create(CDateRange.atLeast(dateReader.parseToLocalDate(row[0])));
			}

			final int startIndex = formats[0];
			final int endIndex = formats[1];

			LocalDate start = dateReader.parseToLocalDate(row[startIndex]);
			LocalDate end = dateReader.parseToLocalDate(row[endIndex]);

			if (start == null && end == null) {
				return null;
			}

			return CDateSet.create(Collections.singleton(CDateRange.of(start, end)));
		}
	},
	DATE_RANGE {
		@Override
		public CDateSet readDates(int[] formats, String[] row, DateReader dateReader) {
			final int index = formats[0];

			return CDateSet.create(Collections.singleton(DateRangeParser.parseISORange(row[index], dateReader)));
		}
	},
	DATE_SET {
		@Override
		public CDateSet readDates(int[] formats, String[] row, DateReader dateReader) {
			final int index = formats[0];

			return CDateSet.parse(row[index], dateReader);
		}
	},
	ALL {
		@Override
		public CDateSet readDates(int[] formats, String[] row, DateReader dateReader) {
			return CDateSet.createFull();
		}
	};

	public abstract CDateSet readDates(int[] formats, String[] row, DateReader dateReader);

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
