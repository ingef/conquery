package com.bakdata.conquery.models.events;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * An empty generic store to avoid any allocations. It still has a length, but {@linkplain #has(int)}} is always false.
 */
@CPSType(base = ColumnStore.class, id = "EMPTY")
public class EmptyStore<T> extends ColumnStore<T> {

	@JsonCreator
	public EmptyStore(){
		super();
		setLines(0);
	}

	@Override
	public long estimateEventBits() {
		return 0;
	}

	@Override
	public EmptyStore<T> doSelect(int[] starts, int[] length) {
		return this;
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
	public boolean isEmpty() {
		return true;
	}
}
