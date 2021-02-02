package com.bakdata.conquery.models.events.stores.root;

public interface BooleanStore extends ColumnStore {

	boolean getBoolean(int event);

	@Override
	default Object createScriptValue(int event) {
		return getBoolean(event);
	}
}
