package com.bakdata.conquery.models.events.stores.root;

public interface RealStore extends ColumnStore {

	double getReal(int event);
	void setReal(int event, double value);

	@Override
	default Object createScriptValue(int event) {
		return getReal(event);
	}
}
