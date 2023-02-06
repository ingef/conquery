package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class IntegerColumnValues extends ColumnValues<Integer> {
	private final IntArrayList values = new IntArrayList();

	protected IntegerColumnValues() {
		super(0);
	}

	@Override
	public Integer get(int event) {
		return values.getInt(event);
	}

	@Override
	protected void append(Integer obj) {
		values.add(obj.intValue());
	}

	@Override
	protected int size() {
		return (int) values.size();
	}
}
