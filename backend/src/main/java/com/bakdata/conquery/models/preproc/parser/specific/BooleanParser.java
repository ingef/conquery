package com.bakdata.conquery.models.preproc.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString(callSuper = true)
public class BooleanParser extends Parser<Boolean, BooleanStore> {

	public BooleanParser(ConqueryConfig config) {
		super(config, new BooleanColumnValues());
	}

	@Override
	protected Boolean parseValue(@Nonnull String value) throws ParsingException {
		return parseBoolean(value);
	}

	@Override
	public int parseAndAdd(String value) throws ParsingException {
		if (value == null) {
			return add(null);
		}

		try {
			return addBoolean(parseBoolean(value));
		}
		catch (Exception e) {
			throw parsingException(value, e);
		}
	}

	public int addBoolean(boolean value) {
		recordBooleanLine(value);
		return ((BooleanColumnValues) requireColumnValues()).addBoolean(value);
	}

	@NotNull
	public static Boolean parseBoolean(@NotNull String value) {
		return switch (value) {
			case "J", "true", "1" -> true;
			case "N", "false", "0" -> false;
			default -> throw new ParsingException("The value " + value + " does not seem to be of type boolean.");
		};
	}

	@Override
	protected BooleanStore decideType() {
		return decideType(getLines());
	}

	@Override
	protected BooleanStore decideType(int storeLines) {
		return BitSetStore.create(storeLines);
	}

	@Override
	public void setValue(BooleanStore store, int event, Boolean value) {
		if(value == null){
			store.setNull(event);
			return;
		}

		store.setBoolean(event, value);
	}

}
