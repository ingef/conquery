package com.bakdata.conquery.models.events.stores.root;

import java.time.LocalDate;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.events.MajorTypeId;

/**
 * {@link ColumnStore} for {@link CDate#ofLocalDate(LocalDate)} values.
 *
 *  * See also {@link MajorTypeId#DATE} and {@link com.bakdata.conquery.models.preproc.parser.specific.DateParser}.
 */
public interface DateStore extends ColumnStore {

	int getDate(int event);
	void setDate(int event, int value);

	@Override
	default Object createScriptValue(int event) {
		return getDate(event);
	}
}
