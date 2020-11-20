package com.bakdata.conquery.models.types.specific.integer;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import lombok.Getter;

@CPSType(base = ColumnStore.class, id = "VAR_INT_INT32")
@Getter
public class VarIntTypeInt extends VarIntType {

	private final int maxValue;
	private final int minValue;

	private final IntegerStore delegate;

	public VarIntTypeInt(int minValue, int maxValue, IntegerStore delegate) {
		super();
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.delegate = delegate;
	}

	@Override
	public VarIntType select(int[] starts, int[] ends) {
		return new VarIntTypeInt(minValue, maxValue, delegate.select(starts, ends));
	}

	@Override
	public int toInt(Long value) {
		return value.intValue();
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Integer.SIZE;
	}
}
