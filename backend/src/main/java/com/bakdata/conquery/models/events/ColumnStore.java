package com.bakdata.conquery.models.events;

import java.math.BigDecimal;

import com.bakdata.conquery.models.common.daterange.CDateRange;

public abstract class ColumnStore {

	public abstract boolean has(int event);

	public abstract int getString(int event);

	public abstract long getInteger(int event);

	public abstract boolean getBoolean(int event);

	public abstract double getReal(int event);

	public abstract BigDecimal getDecimal(int event);

	public abstract long getMoney(int event);

	public abstract int getDate(int event);

	public abstract CDateRange getDateRange(int event);

}
