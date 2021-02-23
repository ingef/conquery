package com.bakdata.conquery.models.events.parser.specific;

import com.bakdata.conquery.models.events.parser.ColumnValues;
import it.unimi.dsi.fastutil.ints.IntBigArrayBigList;

class IntegerColumnValues extends ColumnValues<Integer> {
	private final IntBigArrayBigList values = new IntBigArrayBigList();

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
		return (int) values.size64();
	}
}
