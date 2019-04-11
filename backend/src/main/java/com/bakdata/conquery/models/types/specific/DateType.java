package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base = CType.class, id = "DATE")
public class DateType extends CType<Integer, DateType> {

	public DateType() {
		super(MajorTypeId.DATE, int.class);
	}

	@Override
	protected Integer parseValue(String value) throws ParsingException {
		// see #148 Delegate to DateUtils instead
		return CDate.ofLocalDate(DateFormats.instance().parseToLocalDate(value));
	}

	@Override
	public Object createScriptValue(Integer value) {
		return CDate.toLocalDate(value);
	}

	@Override
	public Object createPrintValue(Integer value) {
		if (value == null) {
			return "";
		}

		return CDate.toLocalDate(value);
	}
}