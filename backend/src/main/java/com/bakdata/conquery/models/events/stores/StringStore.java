package com.bakdata.conquery.models.events.stores;

import java.nio.charset.StandardCharsets;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
@CPSType(id = "STRINGS", base = ColumnStore.class)
public class StringStore extends ColumnStoreAdapter<Integer, StringStore> {

	private final ColumnStore<Long> store;

	private final Dictionary dictionary;

	@JsonCreator
	public StringStore(ColumnStore<Long> store, @NsIdRef Dictionary dictionary) {
		this.store = store;
		this.dictionary = dictionary;
	}

	@Override
	public Object getAsObject(int event) {
		return new String(dictionary.getElement(get(event)), StandardCharsets.UTF_8);
	}

	public static StringStore create(int size, @NsIdRef Dictionary dictionary) {
		return new StringStore(IntegerStore.create(size), dictionary);
	}

	@Override
	public Integer get(int event) {
		return store.get(event).intValue();
	}

	public StringStore select(int[] starts, int[] ends) {
		return new StringStore(store.select(starts, ends), dictionary);
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
