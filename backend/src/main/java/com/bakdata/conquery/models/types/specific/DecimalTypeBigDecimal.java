package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;
import java.util.Arrays;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.DecimalStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="DECIMAL_BIG_DECIMAL")
public class DecimalTypeBigDecimal extends CType<BigDecimal, BigDecimal> {

	public DecimalTypeBigDecimal() {
		super(MajorTypeId.DECIMAL, BigDecimal.class);
	}

	@Override
	public ColumnStore createStore(ImportColumn column, Object[] objects) {
		return new DecimalStore(column, Arrays.stream(objects).map(BigDecimal.class::cast).toArray(BigDecimal[]::new));
	}

	@Override
	public boolean canStoreNull() {
		return true;
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return 500;
	}
}