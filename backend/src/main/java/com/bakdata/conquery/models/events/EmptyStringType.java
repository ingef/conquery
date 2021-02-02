package com.bakdata.conquery.models.events;

import java.util.Collections;
import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringType;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An empty generic store to avoid any allocations. It still has a length, but {@linkplain #has(int)}} is always false.
 */
@CPSType(base = ColumnStore.class, id = "EMPTY_STRING")
public class EmptyStringType extends StringType {

	@JsonCreator
	public EmptyStringType(){
		super();
		setLines(0);
	}

	@Override
	public long estimateEventBits() {
		return 0;
	}

	@Override
	public void set(int event, @Nullable Integer value) {

	}

	@Override
	public Integer get(int event) {
		return null;
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
	public StringType doSelect(int[] starts, int[] length) {
		return this;
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
		return -1;
	}

	@Override
	public Dictionary getUnderlyingDictionary() {
		return null;
	}

	@Override
	public void setUnderlyingDictionary(DictionaryId newDict) {

	}

	@Override
	public void setIndexStore(ColumnStore<Long> newType) {

	}

	@NotNull
	@Override
	public Iterator<String> iterator() {
		return Collections.emptyIterator();
	}
}
