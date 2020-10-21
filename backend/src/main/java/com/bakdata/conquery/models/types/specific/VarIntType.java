package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.models.types.CType;

public abstract class VarIntType extends CType<Integer, Number> {

	public VarIntType() {
		super(null);
	}

	public abstract int toInt(Number value);

	@Override
	public Integer createScriptValue(Number value) {
		return value.intValue();
	}
}
