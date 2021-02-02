package com.bakdata.conquery.models.events.stores.root;

public abstract class DateStore extends ColumnStore<Integer> {

	public abstract int getDate(int event);
}
