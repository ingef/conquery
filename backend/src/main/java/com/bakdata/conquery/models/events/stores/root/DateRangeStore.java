package com.bakdata.conquery.models.events.stores.root;

import com.bakdata.conquery.models.common.daterange.CDateRange;

public interface DateRangeStore extends ColumnStore {

	CDateRange getDateRange(int event);
	void setDateRange(int event, CDateRange value);

	@Override
	default Object createScriptValue(int event) {
		return getDateRange(event).toString();
	}
}
