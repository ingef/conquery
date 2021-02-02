package com.bakdata.conquery.models.events.stores.root;

public interface BooleanStore extends ColumnStore {

	boolean getBoolean(int event);
	void setBoolean(int event, boolean value);

	@Override
	default Object createScriptValue(int event) {
		return getBoolean(event);
	}
}
