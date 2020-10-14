package com.bakdata.conquery.models.events.stores;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
@CPSType(id = "PREFIXED_STRINGS", base = ColumnStore.class)
public class PrefixedStringStore extends ColumnStoreAdapter<Integer, PrefixedStringStore> {

	private final StringStore store;

	private final String prefix;

	@JsonCreator
	public PrefixedStringStore(StringStore store, String prefix) {
		this.store = store;
		this.prefix = prefix;
	}

	@Override
	public Object getAsObject(int event) {
		return prefix + store.getString(event);
	}

	public static PrefixedStringStore create(int size, String prefix, @NsIdRef Dictionary dictionary) {
		return new PrefixedStringStore(StringStore.create(size, dictionary), prefix);
	}

	@Override
	public Integer get(int event) {
		return store.get(event);
	}

	public PrefixedStringStore select(int[] starts, int[] ends) {
		return new PrefixedStringStore(store.select(starts, ends), prefix);
	}

	@Override
	public void set(int event, Integer value) {
		store.set(event,value);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}
