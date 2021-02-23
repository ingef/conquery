package com.bakdata.conquery.models.events.parser.specific;

import java.util.BitSet;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.parser.ColumnValues;
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import lombok.ToString;

@ToString(callSuper = true)
public class BooleanParser extends Parser<Boolean, BooleanStore> {

	public BooleanParser(ParserConfig config) {
		super(config);
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
	protected BooleanStore decideType() {
		return BitSetStore.create(getLines());
	}

	@Override
	public void setValue(BooleanStore store, int event, Boolean value) {
		store.setBoolean(event, value);
	}

	@Override
	public ColumnValues createColumnValues(ParserConfig parserConfig) {
		return new ColumnValues<Boolean>(false) {
			private final BitSet values = new BitSet();

			@Override
			public Boolean get(int event) {
				return values.get(event);
			}


			@Override
			protected int size() {
				return values.cardinality();
			}

			@Override
			protected void append(Boolean obj) {
				values.set(size(), obj ? (byte) 1 : (byte) 0);
			}

		};
	}

}
