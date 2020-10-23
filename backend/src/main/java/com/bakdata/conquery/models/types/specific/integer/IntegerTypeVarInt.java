package com.bakdata.conquery.models.types.specific.integer;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CTypeVarInt;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base=ColumnStore.class, id="INTEGER_VARINT")
@Getter @Setter
public class IntegerTypeVarInt extends CTypeVarInt {


	@JsonCreator
	public IntegerTypeVarInt(VarIntType numberType) {
		super(MajorTypeId.INTEGER, numberType);
	}


	@Override
	public Long createScriptValue(Long value) {
		return numberType.createScriptValue((long) value).longValue();
	}

	@Override
	public Object createPrintValue(Long value) {
		return createScriptValue(value);
	}

	@Override
	public IntegerTypeVarInt select(int[] starts, int[] length) {
		return new IntegerTypeVarInt(numberType.select(starts, length));
	}

}
