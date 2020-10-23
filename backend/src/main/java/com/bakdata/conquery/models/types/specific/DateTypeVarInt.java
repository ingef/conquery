package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.DateStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.CTypeVarInt;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="DATE_COMPRESSED") @Getter @Setter
public class DateTypeVarInt extends CTypeVarInt<Long> {
	
	@JsonCreator
	public DateTypeVarInt(VarIntType numberType) {
		super(MajorTypeId.DATE, numberType);
	}

	@Override
	public Object createScriptValue(Number value) {
		return CDate.toLocalDate(value.intValue());
	}

	@Override
	public Object createPrintValue(Number value) {
		return CDate.toLocalDate(value.intValue());
	}

	@Override
	public ColumnStore createStore(int size) {
		return DateStore.create(size);
	}



	@Override
	public DateTypeVarInt select(int[] starts, int[] length) {
		return null;
	}

	@Override
	public void set(int event, Number value) {

	}

	@Override
	public Number get(int event) {
		return null;
	}

	@Override
	public boolean has(int event) {
		return false;
	}
}