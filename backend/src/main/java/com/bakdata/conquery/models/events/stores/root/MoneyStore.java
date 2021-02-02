package com.bakdata.conquery.models.events.stores.root;

public abstract class MoneyStore extends ColumnStore<Long> {

	public abstract long getMoney(int event);
}
