package com.bakdata.conquery.models.events.stores.root;

import com.bakdata.conquery.models.common.CDate;

public interface DateStore extends ColumnStore {

	int getDate(int event);
	void setDate(int event, int value);

	@Override
	default Object createScriptValue(int event) {
		return CDate.toLocalDate(getDate(event));
	}
}
