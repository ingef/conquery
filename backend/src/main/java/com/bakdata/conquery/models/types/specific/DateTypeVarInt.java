package com.bakdata.conquery.models.types.specific;

import java.time.LocalDate;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.DateStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.CTypeVarInt;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="DATE_COMPRESSED") @Getter @Setter
public class DateTypeVarInt extends CTypeVarInt<Integer> {
	
	@JsonCreator
	public DateTypeVarInt(VarIntType numberType) {
		super(MajorTypeId.DATE, numberType);
	}

	@Override
	public ColumnStore createStore(Integer[] objects) {
		return new DateStore(super.createStore(objects));
	}

	@Override
	public LocalDate createScriptValue(Number value) {
		return CDate.toLocalDate(numberType.toInt(value));
	}

	@Override
	public Object createPrintValue(Number value) {
		if (value == null) {
			return "";
		}

		return CDate.toLocalDate(numberType.toInt(value));
	}
}