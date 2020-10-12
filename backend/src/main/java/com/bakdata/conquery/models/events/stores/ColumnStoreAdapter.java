package com.bakdata.conquery.models.events.stores;


import java.math.BigDecimal;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;

abstract class ColumnStoreAdapter<T, ID extends ColumnStore<T>> implements ColumnStore<T> {

	@Override
	public int getString(int event) {
		return (int) get(event);
	}

	@Override
	public long getInteger(int event) {
		return (long) get(event);
	}

	@Override
	public boolean getBoolean(int event) {
		return (boolean) get(event);
	}

	@Override
	public double getReal(int event) {
		return (double) get(event);
	}

	@Override
	public BigDecimal getDecimal(int event) {
		return (BigDecimal) get(event);
	}

	@Override
	public long getMoney(int event) {
		return (long) get(event);
	}

	@Override
	public int getDate(int event) {
		return (int) get(event);
	}

	@Override
	public CDateRange getDateRange(int event) {
		return (CDateRange) get(event);
	}

	@Override
	public Object getAsObject(int event) {
		return get(event);
	}
}
