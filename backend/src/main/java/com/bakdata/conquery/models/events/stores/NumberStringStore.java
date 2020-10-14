package com.bakdata.conquery.models.events.stores;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
@CPSType(id = "NUMBER_STRINGS", base = ColumnStore.class)
public class NumberStringStore extends ColumnStoreAdapter<Integer, NumberStringStore> {

	private final ColumnStore<Long> store;


	@JsonCreator
	public NumberStringStore(ColumnStore<Long> store) {
		this.store = store;
	}

	@Override
	public Object getAsObject(int event) {
		return Integer.toString(get(event));
	}

	public static NumberStringStore create(int size) {
		return new NumberStringStore(IntegerStore.create(size));
	}

	@Override
	public Integer get(int event) {
		return store.get(event).intValue();
	}

	public NumberStringStore select(int[] starts, int[] ends) {
		return new NumberStringStore(store.select(starts, ends));
	}

	@Override
	public void set(int event, Integer value) {
		if (value == null) {
			store.set(event, null);
			return;
		}
		store.set(event, value.longValue());
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}
