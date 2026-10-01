package com.bakdata.conquery.models.preproc.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.OutputRow;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString(callSuper = true)
public class BooleanParser extends Parser<Boolean, BooleanStore> {

	public BooleanParser(ConqueryConfig config) {
		super(config);
	}

	@Override
	protected Boolean parseValue(@Nonnull String value) throws ParsingException {
		return parseBoolean(value);
	}

	@Override
	public void parse(String value, OutputRow outputRow, int outputIndex) throws ParsingException {
		if (value == null) {
			outputRow.setNull(outputIndex);
			return;
		}

		try {
			outputRow.setBoolean(outputIndex, parseBoolean(value));
		}
		catch (Exception e) {
			throw parsingException(value, e);
		}
	}

	@Override
	public void addLine(OutputRow outputRow, int outputIndex) {
		if (outputRow.isNull(outputIndex)) {
			recordNullLine();
			return;
		}

		recordBooleanLine(outputRow.getBoolean(outputIndex));
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

	@Override
	public ColumnValues createColumnValues() {
		return new BooleanColumnValues();
	}

}
