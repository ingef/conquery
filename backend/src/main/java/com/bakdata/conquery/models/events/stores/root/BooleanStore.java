package com.bakdata.conquery.models.events.stores.root;

public interface BooleanStore extends ColumnStore {

	boolean getBoolean(int event);
}
