package com.bakdata.conquery.models.events.stores;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "DATES", base = ColumnStore.class)
@Getter
public class DateStore extends ColumnStoreAdapter<Integer, DateStore> {

	private final ColumnStore<Integer, ?> store;

	@JsonCreator
	public DateStore(ColumnStore<Integer, ?> store) {
		this.store = store;
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public DateStore merge(List<DateStore> stores) {

		final List<?> collect = stores.stream().map(DateStore::getStore).collect(Collectors.toList());

		final ColumnStore<Integer, ?> values = ((ColumnStore) collect.get(0)).merge(collect);

		return new DateStore(values);
	}


	@Override
	public Integer get(int event) {
		return (int) store.getInteger(event);
	}
}
