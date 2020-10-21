package com.bakdata.conquery.models.events.stores.string;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.ColumnStoreAdapter;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
@CPSType(id = "NUMBER_STRINGS", base = ColumnStore.class)
public class NumberStringStore extends ColumnStoreAdapter<Number> {

	private final ColumnStore<Long> store;


	@JsonCreator
	public NumberStringStore(ColumnStore<Long> store) {
		this.store = store;
	}

	@Override
	public Object getAsObject(int event) {
		return Long.toString(get(event));
	}

	public static NumberStringStore create(int size) {
		return new NumberStringStore(IntegerStore.create(size));
	}

	@Override
	public Long get(int event) {
		return store.getInteger(event);
	}

	public NumberStringStore select(int[] starts, int[] ends) {
		return new NumberStringStore(store.select(starts, ends));
	}

	@Override
	public void set(int event, Number value) {
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
