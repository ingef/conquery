package com.bakdata.conquery.models.events.stores.root;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.MajorTypeId;

/**
 * {@link ColumnStore} for {@link CDateRange}.
 *
 * See also {@link MajorTypeId#DATE_RANGE} and {@link com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser}.

 */
public interface DateRangeStore extends ColumnStore {

	CDateRange getDateRange(int event);
	void setDateRange(int event, CDateRange value);

	@Override
	default Object createScriptValue(int event) {
		return getDateRange(event);
	}
}
