package com.bakdata.conquery.models.events.stores;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "SINGLETON_STRING", base = ColumnStore.class)
public class SingletonStringStore extends ColumnStoreAdapter<Integer, SingletonStringStore>{

	@Getter
	private final String value;
	@Getter
	private final BooleanStore store;

	@JsonCreator
	public SingletonStringStore(String value, BooleanStore nullStore) {
		this.value = value;
		this.store = nullStore;
	}

	public static SingletonStringStore create(int size) {
		return new SingletonStringStore("", BooleanStore.create(size));
	}

	public SingletonStringStore select(int[] starts, int[] ends) {
		return new SingletonStringStore(value, store.select(starts, ends));
	}

	@Override
	public Integer get(int event) {
		if(has(event)){
			return 0;
		}

		throw new IllegalStateException();
	}

	@Override
	public void set(int event, Integer value) {
		store.set(event, value != null);
	}

	@Override
	public boolean has(int event) {
		return store.get(event);
	}
}
