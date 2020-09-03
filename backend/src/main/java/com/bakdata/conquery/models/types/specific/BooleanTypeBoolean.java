package com.bakdata.conquery.models.types.specific;

import java.util.BitSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.BooleanStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="BOOLEAN_BOOLEAN")
public class BooleanTypeBoolean extends CType<Boolean, Boolean> {

	public BooleanTypeBoolean() {
		super(MajorTypeId.BOOLEAN, boolean.class);
	}

	@Override
	public ColumnStore createStore(ImportColumn column, Object[] objects) {
		return new BooleanStore(column, new BitSet(objects.length)); // TODO
	}

	@Override
	public boolean canStoreNull() {
		return false;
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Byte.SIZE;
	}
}