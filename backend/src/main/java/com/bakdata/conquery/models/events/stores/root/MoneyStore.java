package com.bakdata.conquery.models.events.stores.root;

public interface MoneyStore extends ColumnStore {

	long getMoney(int event);
}
