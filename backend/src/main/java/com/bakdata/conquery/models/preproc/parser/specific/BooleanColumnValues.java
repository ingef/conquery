package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.BitSet;

import com.bakdata.conquery.models.preproc.parser.ColumnValues;

class BooleanColumnValues extends ColumnValues<Boolean> {
	private final BitSet values = new BitSet();
	private int size = 0;

	@Override
	public Boolean get(int event) {
		if (event > values.length()) {
			return false;
		}

		return values.get(event);
	}

	@Override
	protected int size() {
		return size;
	}

	@Override
	protected void appendNull() {
		// The coalesced false is masked by nullbits in the resulting store, it's just used to keep the store itself aligned
		size++;
	}

	public void addBoolean(boolean value) {
		prepareAdd(false);
		values.set(size, value);
		size++;
	}

	@Override
	protected void truncateValues(int size) {
		values.clear(size, this.size);
		this.size = size;
	}

}
