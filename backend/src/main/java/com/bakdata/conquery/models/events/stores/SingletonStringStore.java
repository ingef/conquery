package com.bakdata.conquery.models.events.stores;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "SINGLETON_STRING", base = ColumnStore.class)
public class SingletonStringStore extends ColumnStoreAdapter<SingletonStringStore>{

	@Getter
	private final String value;
	@Getter
	private final BooleanStore nullStore;

	@JsonCreator
	public SingletonStringStore(String value, BooleanStore nullStore) {
		this.value = value;
		this.nullStore = nullStore;
	}

	@Override
	public SingletonStringStore merge(List<? extends SingletonStringStore> stores) {
		final List<BooleanStore> collect = stores.stream().map(SingletonStringStore::getNullStore).collect(Collectors.toList());

		final BooleanStore nulls = collect.get(0).merge(collect);


		return new SingletonStringStore(value, nulls);
	}

	@Override
	public int getString(int event) {
		if(has(event)){
			return 0;
		}

		throw new IllegalStateException();
	}

	@Override
	public boolean has(int event) {
		return nullStore.has(event);
	}

	@Override
	public Object getAsObject(int event) {
		if(has(event)){
			return value;
		}

		return "";
	}
}
