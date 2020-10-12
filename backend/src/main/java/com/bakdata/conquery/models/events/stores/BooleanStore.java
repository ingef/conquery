package com.bakdata.conquery.models.events.stores;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;


@CPSType(id = "BOOLEANS", base = ColumnStore.class)
@Getter
public class BooleanStore extends ColumnStoreAdapter<Boolean, BooleanStore> {

	@Getter
	private final boolean[] values;

	@JsonCreator
	public BooleanStore(@NotNull boolean[] values) {
		super();
		this.values = values;
	}

	public static BooleanStore create(int size) {
		return new BooleanStore(new boolean[size]);
	}


	@Override
	public void set(int event, Boolean value) {
		values[event] = value;
	}

	@Override
	public boolean has(int event) {
		return true;
	}

	@Override
	public Boolean get(int event) {
		return values[event];
	}
}
