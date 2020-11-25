package com.bakdata.conquery.models.types.specific.integer;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.ByteStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(base = ColumnStore.class, id = "VAR_INT_BYTE")
@Getter
public class VarIntTypeByte extends VarIntType {

	private final byte maxValue;
	private final byte minValue;

	private final ByteStore delegate;

	public ByteStore getDelegate() {
		return delegate;
	}

	@JsonCreator
	public VarIntTypeByte(byte minValue, byte maxValue, ByteStore delegate) {
		super();
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.delegate = delegate;
	}

	@Override
	public long estimateMemoryFieldSize() {
		return Byte.SIZE;
	}

	@Override
	public VarIntTypeByte select(int[] starts, int[] length) {
		return new VarIntTypeByte(minValue, maxValue, delegate.select(starts, length));
	}

}
