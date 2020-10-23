package com.bakdata.conquery.models.types.specific.date;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CTypeVarInt;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = ColumnStore.class, id = "DATE_COMPRESSED")
@Getter
@Setter
public class DateTypeVarInt extends CTypeVarInt {

	@JsonCreator
	public DateTypeVarInt(VarIntType numberType) {
		super(MajorTypeId.DATE, numberType);
	}

	@Override
	public Object createScriptValue(Long value) {
		return CDate.toLocalDate(value.intValue());
	}

	@Override
	public Object createPrintValue(Long value) {
		return CDate.toLocalDate(value.intValue());
	}


	@Override
	public DateTypeVarInt select(int[] starts, int[] length) {
		return new DateTypeVarInt(numberType.select(starts, length));
	}

}