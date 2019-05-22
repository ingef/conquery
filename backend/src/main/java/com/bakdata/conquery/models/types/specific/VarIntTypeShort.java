package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;

import lombok.Getter;

@CPSType(base=CType.class, id="VAR_INT_INT16")
@Getter
public class VarIntTypeShort extends VarIntType {

	private final short maxValue;
	private final short minValue;
	
	public VarIntTypeShort(short minValue, short maxValue) {
		super(short.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public int toInt(Number value) {
		return value.shortValue();
	}
}
