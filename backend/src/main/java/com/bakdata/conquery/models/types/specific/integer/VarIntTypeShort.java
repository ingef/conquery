package com.bakdata.conquery.models.types.specific.integer;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.ShortStore;
import lombok.Getter;

@CPSType(base= ColumnStore.class, id="VAR_INT_INT16")
@Getter
public class VarIntTypeShort extends VarIntType {

	private final short maxValue;
	private final short minValue;

	private final ShortStore delegate;
	
	public VarIntTypeShort(short minValue, short maxValue, ShortStore delegate) {
		super();
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.delegate = delegate;
	}

	@Override
	public VarIntType select(int[] starts, int[] ends) {
		return new VarIntTypeShort(minValue, maxValue, delegate.select(starts, ends));
	}

	@Override
	public int toInt(Long value) {
		return value.shortValue();
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Short.SIZE;
	}
}
