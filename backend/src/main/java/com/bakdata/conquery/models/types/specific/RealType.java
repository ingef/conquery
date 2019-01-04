package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.NumberParsing;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="REAL")
public class RealType extends CType<Double, RealType> {

	public RealType() {
		super(MajorTypeId.REAL, double.class);
	}
	
	@Override
	protected Double parseValue(String value) throws ParsingException {
		return NumberParsing.parseDouble(value);
	}
}