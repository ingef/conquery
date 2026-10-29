package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class IntegerColumnValues extends ColumnValues<Integer> {
	private final IntArrayList values = new IntArrayList();

	@Override
	public Integer get(int event) {
		return values.getInt(event);
	}

	public int getInt(int event) {
		return values.getInt(event);
	}

	@Override
	protected void appendNull() {
		values.add(0);
	}

	public void addInt(int value) {
		prepareAdd(false);
		values.add(value);
	}

	@Override
	protected void truncateValues(int size) {
		values.removeElements(size, values.size());
	}

	@Override
	protected int size() {
		return (int) values.size();
	}
}
