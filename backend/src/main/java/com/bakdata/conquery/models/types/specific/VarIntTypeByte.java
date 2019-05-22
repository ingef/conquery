package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;

import lombok.Getter;

@CPSType(base=CType.class, id="VAR_INT_BYTE")
@Getter
public class VarIntTypeByte extends VarIntType {

	private final byte maxValue;
	private final byte minValue;
	
	public VarIntTypeByte(byte minValue, byte maxValue) {
		super(byte.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public int toInt(Number value) {
		return value.byteValue();
	}
}
