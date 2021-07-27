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

	/**
	 * Try and parse value using dateReader, adding result into out set.
	 */
	public abstract void readDates(String value, DateReader dateReader, CDateSet out);

}
