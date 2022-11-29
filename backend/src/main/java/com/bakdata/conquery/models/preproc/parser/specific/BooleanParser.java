package com.bakdata.conquery.models.preproc.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;

@ToString(callSuper = true)
public class BooleanParser extends Parser<Boolean, BooleanStore> {

	public BooleanParser(ConqueryConfig config) {
		super(config);
	}

	@Override
	protected Boolean parseValue(@Nonnull String value) throws ParsingException {
		return switch (value) {
			case "J", "true", "1" -> true;
			case "N", "false", "0" -> false;
			default -> throw new ParsingException("The value " + value + " does not seem to be of type boolean.");
		};
	}

	@Override
	protected BooleanStore decideType() {
		return BitSetStore.create(getLines());
	}

	@Override
	public void setValue(BooleanStore store, int event, Boolean value) {
		store.setBoolean(event, value);
	}

	@Override
	public ColumnValues createColumnValues() {
		return new BooleanColumnValues();
	}

}
