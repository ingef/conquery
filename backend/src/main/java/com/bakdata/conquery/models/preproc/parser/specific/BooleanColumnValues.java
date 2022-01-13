package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.BitSet;

import com.bakdata.conquery.models.preproc.parser.ColumnValues;

class BooleanColumnValues extends ColumnValues<Boolean> {
	private final BitSet values = new BitSet();
	private int size = 0;

	public BooleanColumnValues() {
		super(null);
	}

	@Override
	public Boolean get(int event) {
		if (event > values.length()){
			return false;
		}

		return values.get(event);
	}

	@Override
	protected int size() {
		return size;
	}

	@Override
	protected void append(Boolean obj) {
		// by counting events, we ensure dangling false values that are not counted towards length are respected in the output.
		values.set(size++, obj);
	}

}
