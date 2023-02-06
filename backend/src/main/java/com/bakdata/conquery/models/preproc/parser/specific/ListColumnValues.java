package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.preproc.parser.ColumnValues;

class ListColumnValues<T> extends ColumnValues<T> {
	final List<T> objects = new ArrayList<>();

	public ListColumnValues() {
		super(null);
	}

	@Override
	public T get(int event) {
		return objects.get(event);
	}

	@Override
	protected void append(T obj) {
		objects.add(obj);
	}

	@Override
	protected int size() {
		return objects.size();
	}
}
