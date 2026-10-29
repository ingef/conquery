package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.preproc.parser.ColumnValues;

abstract class ListColumnValues<T> extends ColumnValues<T> {
	protected final List<T> objects = new ArrayList<>();

	@Override
	public T get(int event) {
		return objects.get(event);
	}

	@Override
	protected void appendNull() {
		objects.addLast(null);
	}

	@Override
	protected void truncateValues(int size) {
		objects.subList(size, objects.size()).clear();
	}

	@Override
	protected int size() {
		return objects.size();
	}
}
