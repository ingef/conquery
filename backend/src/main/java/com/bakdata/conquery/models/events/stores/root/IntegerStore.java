package com.bakdata.conquery.models.events.stores.root;

public abstract class IntegerStore extends ColumnStore<Long> {

	@Override
	public abstract IntegerStore doSelect(int[] starts, int[] lengths);

	public abstract long getInteger(int event);
}
