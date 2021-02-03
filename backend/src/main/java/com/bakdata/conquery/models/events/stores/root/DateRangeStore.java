package com.bakdata.conquery.models.events.stores.root;

import com.bakdata.conquery.models.common.daterange.CDateRange;

/**
 * {@link ColumnStore} for {@link CDateRange}.
 *
 * See also {@link com.bakdata.conquery.models.events.parser.MajorTypeId#DATE_RANGE} and {@link com.bakdata.conquery.models.events.parser.specific.DateRangeParser}.

 */
public interface DateRangeStore extends ColumnStore {

	CDateRange getDateRange(int event);
	void setDateRange(int event, CDateRange value);

	@Override
	default Object createScriptValue(int event) {
		return getDateRange(event).toString();
	}
}
