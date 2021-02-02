package com.bakdata.conquery.models.events.stores.root;

public abstract class BooleanStore extends ColumnStore<Boolean> {

	public abstract boolean getBoolean(int event);
}
