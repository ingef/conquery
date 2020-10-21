package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.BooleanStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="BOOLEAN_BOOLEAN")
public class BooleanTypeBoolean extends CType<Boolean, Boolean> {

	public BooleanTypeBoolean() {
		super(MajorTypeId.BOOLEAN);
	}

	@Override
	public ColumnStore createStore(int size) {
		return BooleanStore.create(size);
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Byte.SIZE;
	}
}