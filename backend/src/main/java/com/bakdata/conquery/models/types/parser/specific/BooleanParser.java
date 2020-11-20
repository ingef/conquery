package com.bakdata.conquery.models.types.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.base.BooleanStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.BooleanTypeBoolean;
import lombok.ToString;

@ToString(callSuper = true)
public class BooleanParser extends Parser<Boolean> {

	public BooleanParser(ParserConfig config) {

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
				throw new ParsingException("The value " + value + " does not seem to be of type boolean.");
		}
	}

	@Override
	protected CType<Boolean> decideType() {
		return new BooleanTypeBoolean(BooleanStore.create(getLines()));
	}
}
