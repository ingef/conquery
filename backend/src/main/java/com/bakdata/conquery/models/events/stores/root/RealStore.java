package com.bakdata.conquery.models.events.stores.root;

public abstract class RealStore extends ColumnStore<Double> {

	public abstract double getReal(int event);
}
