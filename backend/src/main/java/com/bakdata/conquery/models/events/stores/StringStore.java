package com.bakdata.conquery.models.events.stores;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
@CPSType(id = "STRINGS", base = ColumnStore.class)
public class StringStore extends ColumnStoreAdapter<StringStore> {

	private final ColumnStore<?> store;

	@JsonCreator
	public StringStore(ColumnStore<?> store) {
		this.store = store;
	}

	@Override
	public StringStore merge(List<? extends StringStore> stores) {
		final List<ColumnStore> collect = stores.stream().map(StringStore::getStore).collect(Collectors.toList());

		final ColumnStore values = collect.get(0).merge(collect);

		return new StringStore(values);
	}

	@Override
	public int getString(int event) {
		return (int) store.getInteger(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public Object getAsObject(int event) {
		return getString(event);
	}
}
