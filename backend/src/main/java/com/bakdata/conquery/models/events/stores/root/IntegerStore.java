package com.bakdata.conquery.models.events.stores.root;

public interface IntegerStore extends ColumnStore {

	long getInteger(int event);
	void setInteger(int event, long value);

	@Override
	default Object createScriptValue(int event) {
		return getInteger(event);
	}
}
