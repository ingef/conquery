package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.bakdata.conquery.util.DateFormats;
import com.google.common.base.Preconditions;
import com.google.common.collect.MoreCollectors;
import org.apache.commons.collections.EnumerationUtils;

public enum DateFormat {
	EVENT_DATE {
		@Override
		public CDateSet readDates(FormatColumn[] formats, String[] row, DateFormats dateFormats) {
			final int index = findIndex(formats, DateColumn.EventDate.class);

			return CDateSet.create(Collections.singleton(CDateRange.exactly(dateFormats.parseToLocalDate(row[index]))));
		}
	},
	START_END_DATE {
		@Override
		public CDateSet readDates(FormatColumn[] formats, String[] row, DateFormats dateFormats) {

			final int startIndex = findIndex(formats, DateColumn.StartDate.class);
			final int endIndex = findIndex(formats, DateColumn.EndDate.class);

			LocalDate start = dateFormats.parseToLocalDate(row[startIndex]);
			LocalDate end = dateFormats.parseToLocalDate(row[endIndex]);

			if (start == null && end == null) {
				return null;
			}

			return CDateSet.create(Collections.singleton(CDateRange.of(start, end)));
		}
	},
	DATE_RANGE {
		@Override
		public CDateSet readDates(FormatColumn[] formats, String[] row, DateFormats dateFormats) {
			final int index = findIndex(formats, DateColumn.DateRange.class);

			return CDateSet.create(Collections.singleton(DateRangeParser.parseISORange(row[index], dateFormats)));
		}
	},
	DATE_SET {
		@Override
		public CDateSet readDates(FormatColumn[] formats, String[] row, DateFormats dateFormats) {
			final int index = findIndex(formats, DateColumn.DateSet.class);

			return CDateSet.parse(row[index], dateFormats);
		}
	},
	ALL {
		@Override
		public CDateSet readDates(FormatColumn[] formats, String[] row, DateFormats dateFormats) {
			return CDateSet.createFull();
		}
	};

	public abstract CDateSet readDates(FormatColumn[] formats, String[] row, DateFormats dateFormats);

	protected static int findIndex(FormatColumn[] formats, Class<? extends DateColumn> clazz){
		for (int index = 0; index < formats.length; index++) {
			if (clazz.isInstance(formats[index])) {
				return index;
			}
		}

		return -1;
	}
}
