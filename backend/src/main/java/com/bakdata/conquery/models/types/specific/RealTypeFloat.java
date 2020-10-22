package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.FloatStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="REAL_FLOAT")
public class RealTypeFloat extends CType<Double, Float> {

	public RealTypeFloat() {
		super(MajorTypeId.REAL);
	}

	@Override
	public ColumnStore createStore(int size) {
		return FloatStore.create(size);
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Float.SIZE;
	}
}