package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="REAL_FLOAT")
public class RealTypeFloat extends CType<Double, Float> {

	public RealTypeFloat() {
		super(MajorTypeId.REAL, float.class);
	}

	@Override
	public boolean canStoreNull() {
		return true;
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Float.SIZE;
	}
}