package com.bakdata.conquery.models.events.stores.root;

public interface RealStore extends ColumnStore {

	double getReal(int event);

	@Override
	default Object createScriptValue(int event) {
		return getReal(event);
	}
}
