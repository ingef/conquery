package com.bakdata.conquery.models.events.stores;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;


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

	@Override
	public BooleanStore merge(List<BooleanStore> stores) {
		boolean[] out = new boolean[0];

		// naive impl might be slow because it reallocates very often.
		for (BooleanStore store : stores) {
			out = ArrayUtils.addAll(out, store.getValues());
		}

		return new BooleanStore(out);
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
