package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

class LongColumnValues extends ColumnValues<Long> {
	private final LongList values = new LongArrayList();

	@Override
	public Long get(int event) {
		return values.getLong(event);
	}

	@Override
	protected void appendNull() {
		values.add(0L);
	}

	public void addLong(long value) {
		prepareAdd(false);
		values.add(value);
	}

	@Override
	protected void truncateValues(int size) {
		values.removeElements(size, values.size());
	}

	@Override
	protected int size() {
		return values.size();
	}
}
