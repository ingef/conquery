package com.bakdata.conquery.models.types.specific;

import java.util.Arrays;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.DoubleStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="REAL_DOUBLE")
public class RealTypeDouble extends CType<Double, Double> {

	public RealTypeDouble() {
		super(MajorTypeId.REAL, double.class);
	}

	@Override
	public boolean canStoreNull() {
		return false;
	}

	@Override
	public ColumnStore createStore(ImportColumn column, Object[] objects) {
		return new DoubleStore(column, Arrays.stream(objects).mapToDouble(Double.class::cast).toArray());
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Double.SIZE;
	}
}