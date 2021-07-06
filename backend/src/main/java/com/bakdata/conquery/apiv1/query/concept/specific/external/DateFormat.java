package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.bakdata.conquery.util.DateFormats;
import com.google.common.base.Preconditions;
import com.google.common.collect.MoreCollectors;

public enum DateFormat {
	EVENT_DATE {
		@Override
		public CDateSet readDates(DateColumn[] dateIndices, String[] row, DateFormats dateFormats) {
			Preconditions.checkArgument(dateIndices.length == 1);

			return CDateSet.create(Collections.singleton(CDateRange.exactly(dateFormats.parseToLocalDate(row[dateIndices[0].getPosition()]))));
		}
	},
	START_END_DATE {
		@Override
		public CDateSet readDates(DateColumn[] dateIndices, String[] row, DateFormats dateFormats) {
			Preconditions.checkArgument(dateIndices.length == 1 || dateIndices.length == 2);


			LocalDate start = Arrays.stream(dateIndices)
									.filter(DateColumn.DateStart.class::isInstance)
									.map(DateColumn::getPosition)
									.map(idx -> row[idx])
									.map(dateFormats::parseToLocalDate)
									.collect(MoreCollectors.toOptional())
									.orElse(null);

			LocalDate end = Arrays.stream(dateIndices)
								  .filter(DateColumn.DateEnd.class::isInstance)
								  .map(DateColumn::getPosition)
								  .map(idx -> row[idx])
								  .map(dateFormats::parseToLocalDate)
								  .collect(MoreCollectors.toOptional())
								  .orElse(null);

			if (start == null && end == null) {
				return null;
			}

			return CDateSet.create(Collections.singleton(CDateRange.of(start, end)));
		}
	},
	DATE_RANGE {
		@Override
		public CDateSet readDates(DateColumn[] dateIndices, String[] row, DateFormats dateFormats) {
			Preconditions.checkArgument(dateIndices.length == 1);

			return CDateSet.create(Collections.singleton(DateRangeParser.parseISORange(row[dateIndices[0].getPosition()], dateFormats)));
		}
	},
	DATE_SET {
		@Override
		public CDateSet readDates(DateColumn[] dateIndices, String[] row, DateFormats dateFormats) {
			Preconditions.checkArgument(dateIndices.length == 1);

			return CDateSet.parse(row[dateIndices[0].getPosition()], dateFormats);
		}
	},
	ALL {
		@Override
		public CDateSet readDates(DateColumn[] dateIndices, String[] row, DateFormats dateFormats) {
			Preconditions.checkArgument(dateIndices.length == 0);

			return CDateSet.createFull();
		}
	};

	public abstract CDateSet readDates(DateColumn[] dateIndices, String[] row, DateFormats dateFormats);
}
