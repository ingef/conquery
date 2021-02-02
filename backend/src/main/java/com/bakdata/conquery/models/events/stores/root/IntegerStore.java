package com.bakdata.conquery.models.events.stores.root;

public interface IntegerStore extends ColumnStore {

	long getInteger(int event);
}
