package com.bakdata.conquery.models.events;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.types.ColumnStore;
import com.bakdata.conquery.models.events.stores.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(base = ColumnStore.class, id = "EMPTY")
public class EmptyStore<T> extends ColumnStore<T> {

	/**
	 * Necessary for deSerialization
	 */
	public MajorTypeId getType(){
		return super.getTypeId();
	}

	@JsonCreator
	public EmptyStore(MajorTypeId type){
		super(type);
	}

	@Override
	public long estimateEventBytes() {
		return 0;
	}

	@Override
	public EmptyStore<T> select(int[] starts, int[] length) {
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
}
