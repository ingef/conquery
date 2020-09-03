package com.bakdata.conquery.models.events.stores;


import java.math.BigDecimal;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;

abstract class ColumnStoreAdapter extends ColumnStore {
	protected ColumnStoreAdapter() { }

	@Override
	public int getString(int event) {
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public long getInteger(int event) {
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public boolean getBoolean(int event) {
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public double getReal(int event) {
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public BigDecimal getDecimal(int event) {
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public long getMoney(int event) {
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public int getDate(int event) {
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public CDateRange getDateRange(int event) {
		throw new IllegalArgumentException("Not Implemented");
	}
}
