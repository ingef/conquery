package com.bakdata.conquery.models.events.stores.root;

public interface MoneyStore extends ColumnStore {

	long getMoney(int event);

	@Override
	default Object createScriptValue(int event) {
		return getMoney(event);
	}
}
