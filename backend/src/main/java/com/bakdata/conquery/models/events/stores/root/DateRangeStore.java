package com.bakdata.conquery.models.events.stores.root;

import com.bakdata.conquery.models.common.daterange.CDateRange;

public abstract class DateRangeStore extends ColumnStore<CDateRange> {

	public abstract CDateRange getDateRange(int event);
}
