package com.bakdata.conquery.models.events.stores.root;

public interface MoneyStore extends ColumnStore {

	long getMoney(int event);
	void setMoney(int event, long money);

	@Override
	default Object createScriptValue(int event) {
		return getMoney(event);
	}
}
