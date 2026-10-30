package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.Objects;

import com.bakdata.conquery.models.common.daterange.CDateRange;

class DateRangeColumnValues extends ListColumnValues<CDateRange> {

	public void addDateRange(CDateRange value) {
		Objects.requireNonNull(value, "value");

		prepareAdd(false);
		objects.add(value);
	}
}
