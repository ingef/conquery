package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

class LongColumnValues extends ColumnValues<Long> {
	private final LongList values = new LongArrayList();

	protected LongColumnValues() {
		super(0L);
	}

	@Override
	public Long get(int event) {
		return values.getLong(event);
	}

	@Override
	protected void append(Long obj) {
		values.add(obj.longValue());
	}

	@Override
	protected int size() {
		return values.size();
	}
}
