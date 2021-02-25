package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.BitSet;

import com.bakdata.conquery.models.preproc.parser.ColumnValues;

class BooleanColumnValues extends ColumnValues<Boolean> {
	private final BitSet values = new BitSet();

	public BooleanColumnValues() {
		super(false);
	}

	@Override
	public Boolean get(int event) {
		return values.get(event);
	}


	@Override
	protected int size() {
		return values.cardinality();
	}

	@Override
	protected void append(Boolean obj) {
		values.set(size(), obj);
	}

}
