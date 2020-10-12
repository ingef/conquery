package com.bakdata.conquery.models.events.stores;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
@CPSType(id = "STRINGS", base = ColumnStore.class)
public class StringStore extends ColumnStoreAdapter<Integer, StringStore> {

	private final ColumnStore<Long> store;

	@JsonCreator
	public StringStore(ColumnStore<Long> store) {
		this.store = store;
	}

	public static StringStore create(int size) {
		return new StringStore(IntegerStore.create(size));
	}

	@Override
	public Integer get(int event) {
		return store.get(event).intValue();
	}

	@Override
	public void set(int event, Integer value) {
		if(value == null){
			store.set(event,null);
			return;
		}
		store.set(event, value.longValue());
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}
