package com.bakdata.conquery.models.events.parser.specific;

import com.bakdata.conquery.models.events.parser.ColumnValues;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.fastutil.longs.LongBigList;

class LongColumnValues extends ColumnValues<Long> {
	private final LongBigList values = new LongBigArrayBigList();

	protected LongColumnValues() {
		super(0L);
	}

	@Override
	public Long read(int event) {
		return values.getLong(event);
	}

	@Override
	protected void write(Long obj) {
		values.add(obj.longValue());
	}

	@Override
	protected int countValues() {
		return (int) values.size64();
	}
}
