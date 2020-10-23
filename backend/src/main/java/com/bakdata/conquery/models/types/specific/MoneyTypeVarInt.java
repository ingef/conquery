package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CTypeVarInt;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = ColumnStore.class, id = "MONEY_VARINT")
@Getter
@Setter
public class MoneyTypeVarInt extends CTypeVarInt<Long> {

	@JsonCreator
	public MoneyTypeVarInt(VarIntType numberType) {
		super(MajorTypeId.MONEY, numberType);
	}

	@Override
	public Long createScriptValue(Long value) {
		return (long) numberType.createScriptValue(value);
	}

	@Override
	public Object createPrintValue(Long value) {
		return createScriptValue(value);
	}

	@Override
	public MoneyTypeVarInt select(int[] starts, int[] length) {
		return new MoneyTypeVarInt(numberType.select(starts, length));
	}

	@Override
	public Long get(int event) {
		return numberType.get(event);
	}
}
