package com.bakdata.conquery.models.events;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DecimalStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.bakdata.conquery.models.events.stores.root.RealStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An empty generic store to avoid any allocations. It still has a length, but {@linkplain #has(int)}} is always false.
 */
@CPSType(base = ColumnStore.class, id = "EMPTY")
public class EmptyStore implements
		IntegerStore, RealStore, BooleanStore, DecimalStore, StringStore, MoneyStore
{

	@JsonCreator
	public EmptyStore(){
		super();
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
	public void set(int event, @Nullable Object value) {

	}

	@Override
	public Object createScriptValue(int event) {
		return null;
	}

	@Override
	public Long get(int event) {
		return null;
	}

	@Override
	public EmptyStore select(int[] starts, int[] length) {
		return this;
	}

	@Override
	public boolean has(int event) {
		return false;
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
	public BigDecimal getDecimal(int event) {
		return BigDecimal.ZERO;
	}

	@Override
	public long getInteger(int event) {
		return 0;
	}

	@Override
	public long getMoney(int event) {
		return 0;
	}

	@Override
	public double getReal(int event) {
		return 0;
	}

	@Override
	public int getString(int event) {
		return 0;
	}

	@Override
	public String getElement(int id) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public int getId(String value) {
		return 0;
	}

	@Override
	public Dictionary getUnderlyingDictionary() {
		return null;
	}

	@Override
	public void setUnderlyingDictionary(DictionaryId newDict) {

	}

	@Override
	public void setIndexStore(IntegerStore newType) {

	}

	@NotNull
	@Override
	public Iterator<String> iterator() {
		return Collections.emptyIterator();
	}
}
