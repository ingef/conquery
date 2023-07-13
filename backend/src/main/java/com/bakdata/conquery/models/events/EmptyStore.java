package com.bakdata.conquery.models.events;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.root.DecimalStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.bakdata.conquery.models.events.stores.root.RealStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;

/**
 * An empty generic store to avoid any allocations.
 *
 * @implNote this class is a singleton.
 */
@CPSType(base = ColumnStore.class, id = "EMPTY")
public enum EmptyStore implements
		IntegerStore, RealStore, BooleanStore, DecimalStore, StringStore, MoneyStore, DateStore {
	INSTANCE;

	@Override
	public String toString() {
		return "EmptyStore()";
	}

	@Override
	public int getLines() {
		return 0;
	}

	@Override
	public long estimateEventBits() {
		return 0;
	}


	@Override
	public int getDate(int event) {
		return 0;
	}

	@Override
	public void setDate(int event, int value) {

	}

	@Override
	public Object createScriptValue(int event) {
		return null;
	}

	@Override
	public EmptyStore select(int[] starts, int[] length) {
		return this;
	}

	@Override
	public EmptyStore createDescription() {
		return this;
	}

	@Override
	public boolean has(int event) {
		return false;
	}

	@Override
	public void setNull(int event) {

	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean getBoolean(int event) {
		return false;
	}

	@Override
	public void setBoolean(int event, boolean value) {

	}

	@Override
	public BigDecimal getDecimal(int event) {
		return BigDecimal.ZERO;
	}

	@Override
	public void setDecimal(int event, BigDecimal value) {

	}

	@Override
	public long getInteger(int event) {
		return 0;
	}

	@Override
	public void setInteger(int event, long value) {

	}

	@Override
	public long getMoney(int event) {
		return 0;
	}

	@Override
	public void setMoney(int event, long money) {

	}

	@Override
	public double getReal(int event) {
		return 0;
	}

	@Override
	public void setReal(int event, double value) {

	}

	@Override
	public String getString(int event) {
		return null;
	}

	@Override
	public void setString(int event, String value) {

	}

	private String getElement(int id) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Stream<String> iterateValues() {
		return Stream.empty();
	}

}
