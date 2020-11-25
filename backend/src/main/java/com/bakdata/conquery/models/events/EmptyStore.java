package com.bakdata.conquery.models.events;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;

@CPSType(base = ColumnStore.class, id = "EMPTY")
public class EmptyStore<T> implements ColumnStore<T> {

	//TODO FK: Implement usage of this
	private static final EmptyStore INSTANCE = new EmptyStore();

	public static <T> EmptyStore<T> getInstance() {
		return (EmptyStore<T>) INSTANCE;
	}

	private EmptyStore(){

	}

	@Override
	public ColumnStore select(int[] starts, int[] length) {
		return null;
	}

	@Override
	public void set(int event, T value) {

	}

	@Override
	public T get(int event) {
		return null;
	}

	@Override
	public boolean has(int event) {
		return false;
	}

	@Override
	public int getString(int event) {
		return 0;
	}

	@Override
	public long getInteger(int event) {
		return 0;
	}

	@Override
	public boolean getBoolean(int event) {
		return false;
	}

	@Override
	public double getReal(int event) {
		return 0;
	}

	@Override
	public BigDecimal getDecimal(int event) {
		return null;
	}

	@Override
	public long getMoney(int event) {
		return 0;
	}

	@Override
	public int getDate(int event) {
		return 0;
	}

	@Override
	public CDateRange getDateRange(int event) {
		return null;
	}

	@Override
	public Object getAsObject(int event) {
		return null;
	}
}
