package com.bakdata.conquery.models.events.stores.root;

public interface RealStore extends ColumnStore {

	double getReal(int event);
}
