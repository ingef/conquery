package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.regex.Pattern;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.stores.primitive.StringStoreString;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Analyze all strings for common suffix/prefix, or if they are singleton.
 */
@Slf4j
@Getter
@ToString(callSuper = true)
public class StringParser extends Parser<String, StringStore> {


	private static final Pattern DIGITS = Pattern.compile("^\\d+$");

	public StringParser(ConqueryConfig config) {
		super(config);
	}

	@Override
	protected String parseValue(String value) throws ParsingException {
		return value.intern();
	}

	@Override
	protected void registerValue(String v) {

	}

	@Override
	protected StringStore decideType() {

		//check if a singleton type is enough
		if (getLines() == 0) {
			return EmptyStore.INSTANCE;
		}

		return StringStoreString.create(getLines());
	}




	@Override
	public void setValue(StringStore store, int event, String value) {
		store.setString(event, value);
	}

	@SneakyThrows
	@Override
	public ColumnValues<String> createColumnValues() {
		return new StringColumnValues();
	}

}
