package com.bakdata.conquery.models.events.stores.base;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.ColumnStoreAdapter;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;


@CPSType(id = "BOOLEANS", base = ColumnStore.class)
@Getter
public class BooleanStore extends ColumnStoreAdapter<Boolean> {



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

	public BooleanStore select(int[] starts, int[] ends) {
		// todo use bitset
		return new BooleanStore(ColumnStore.selectArray(starts, ends, values, boolean[]::new));
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
