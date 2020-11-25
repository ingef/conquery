package com.bakdata.conquery.models.events.stores.base;

import java.util.BitSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.ColumnStoreAdapter;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;


@CPSType(id = "BOOLEANS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class BooleanStore extends ColumnStoreAdapter<Boolean> {

	private final BitSet values;

	@JsonCreator
	public BooleanStore(BitSet values) {
		super();
		this.values = values;
	}

	public static BooleanStore create(int size) {
		return new BooleanStore(new BitSet(size));
	}

	public BooleanStore select(int[] starts, int[] ends) {
		return new BooleanStore(ColumnStore.selectArray(starts, ends, values, BitSet::new));
	}

	@Override
	public void set(int event, Boolean value) {
		if (value == null) {
			set(event, false);
			return;
		}

		values.set(event, value);
	}

	@Override
	public boolean has(int event) {
		return true;
	}

	@Override
	public Boolean get(int event) {
		return values.get(event);
	}
}
