package com.bakdata.conquery.models.types.specific;

import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="DATE_RANGE")
public class DateRangeType extends CType<CDateRange, DateRangeType> {

	public DateRangeType() {
		super(MajorTypeId.DATE_RANGE, CDateRange.class);
	}

	@Override
	protected CDateRange parseValue(String value) throws ParsingException {
		return parseISORange(value);
	}

	public static CDateRange parseISORange(String value) throws ParsingException {
		if(value==null) {
			return null;
		}
		String[] parts = StringUtils.split(value, '/');
		if(parts.length!=2) {
			throw ParsingException.of(value, "daterange");
		}
		DateFormats formats = DateFormats.instance();


		return CDateRange.of(
				formats.parseToLocalDate(parts[0]),
				formats.parseToLocalDate(parts[1])
		);
	}

	@Override
	public Object createPrintValue(CDateRange value) {
		if (value == null) {
			return "";
		}

		return value;
	}
}