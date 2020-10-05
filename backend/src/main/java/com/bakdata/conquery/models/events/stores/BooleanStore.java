package com.bakdata.conquery.models.events.stores;

import java.io.OutputStream;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;


@CPSType(id = "BOOLEANS", base = ColumnStore.class)
@Getter
public class BooleanStore extends ColumnStoreAdapter<BooleanStore> {

	private final boolean[] values;

	@JsonCreator
	public BooleanStore(@NotNull boolean[] values) {
		super();
		this.values = values;
	}

	@Override
	public BooleanStore merge(List<? extends BooleanStore> stores) {

		final int newSize = stores.stream().map(BooleanStore.class::cast).mapToInt(store -> store.values.length).sum();
		final boolean[] mergedValues = new boolean[newSize];

		int start = 0;

		//TODO !

		return new BooleanStore(mergedValues);
	}

	@Override
	public boolean has(int event) {
		return true;
	}

	@Override
	public boolean getBoolean(int event) {
		return values[event];
	}

	@Override
	public Object getAsObject(int event) {
		return getBoolean(event);
	}

	@Override
	public void serialize(OutputStream outputStream) {

	}
}
