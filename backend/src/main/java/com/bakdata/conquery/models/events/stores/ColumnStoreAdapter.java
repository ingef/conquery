package com.bakdata.conquery.models.events.stores;


import java.io.OutputStream;
import java.math.BigDecimal;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;

abstract class ColumnStoreAdapter<T, ID extends ColumnStore<T, ? extends ColumnStore<T, ID>>> implements ColumnStore<T, ID> {

	@Override
	public final int getString(int event) {
		return (int) get(event);
	}

	@Override
	public final long getInteger(int event){
		return (long) get(event);
	}
	@Override
	public final boolean getBoolean(int event){
		return (boolean) get(event);
	}
	@Override
	public final double getReal(int event){
		return (double) get(event);
	}
	@Override
	public final BigDecimal getDecimal(int event){
		return (BigDecimal) get(event);
	}
	@Override
	public final long getMoney(int event){
		return (long) get(event);
	}
	@Override
	public final int getDate(int event){
		return (int) get(event);
	}
	@Override
	public final CDateRange getDateRange(int event){
		return (CDateRange) get(event);
	}

	@Override
	public final Object getAsObject(int event) {
		return get(event);
	}

	@Override
	public void serialize(OutputStream outputStream) {

	}
}
