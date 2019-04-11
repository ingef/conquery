package com.bakdata.conquery.models.types.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="BOOLEAN")
public class BooleanType extends CType<Boolean, BooleanType> {

	public BooleanType() {
		super(MajorTypeId.BOOLEAN, boolean.class);
	}

	@Override
	protected Boolean parseValue(@Nonnull String value) throws ParsingException {
		switch (value) {
			case "J":
			case "true":
			case "1":
				return true;
			case "N":
			case "false":
			case "0":
				return false;
			default:
				throw new ParsingException("The value "+value+" does not seem to be of type boolean.");
		}
	}
}