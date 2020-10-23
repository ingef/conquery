package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.BooleanStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=ColumnStore.class, id="BOOLEAN_BOOLEAN")
public class BooleanTypeBoolean extends CType<Boolean, Boolean> {

	private final BooleanStore store;

	public BooleanTypeBoolean(BooleanStore store) {
		super(MajorTypeId.BOOLEAN);
		this.store = store;
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Byte.SIZE;
	}

	@Override
	public BooleanTypeBoolean select(int[] starts, int[] length) {
		return new BooleanTypeBoolean(store.select(starts, length));
	}

	@Override
	public void set(int event, Boolean value) {
		store.set(event, value);
	}

	@Override
	public Boolean get(int event) {
		return store.get(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}