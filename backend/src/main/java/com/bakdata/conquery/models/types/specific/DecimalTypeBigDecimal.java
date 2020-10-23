package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.DecimalStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="DECIMAL_BIG_DECIMAL")
public class DecimalTypeBigDecimal extends CType<BigDecimal, BigDecimal> {

	public DecimalTypeBigDecimal() {
		super(MajorTypeId.DECIMAL);
	}

	@Override
	public ColumnStore createStore(int size) {
		return DecimalStore.create(size);
	}

	@Override
	public long estimateMemoryBitWidth() {
		return 500;
	}

	@Override
	public ColumnStore<BigDecimal> select(int[] starts, int[] length) {
		return null;
	}

	@Override
	public void set(int event, BigDecimal value) {

	}

	@Override
	public BigDecimal get(int event) {
		return null;
	}

	@Override
	public boolean has(int event) {
		return false;
	}
}