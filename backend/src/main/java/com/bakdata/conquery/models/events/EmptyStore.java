package com.bakdata.conquery.models.events;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(base = ColumnStore.class, id = "EMPTY")
public class EmptyStore implements ColumnStore {

	//TODO FK: Implement usage of this
	@Getter(onMethod_ = JsonCreator.class)
	private static EmptyStore Instance = new EmptyStore();

	private EmptyStore(){

	}

	@Override
	public ColumnStore select(int[] starts, int[] length) {
		return null;
	}

	@Override
	public void set(int event, Object value) {

	}

	@Override
	public Object get(int event) {
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
