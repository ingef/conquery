package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.time.LocalDate;
import java.util.Collections;

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
	START_END_DATE {
		@Override
		public CDateSet readDates(int[] formats, String[] row, DateReader dateReader) {

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

}
