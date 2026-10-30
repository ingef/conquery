package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.Objects;
import java.util.regex.Pattern;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.stores.primitive.StringStoreString;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Analyze all strings for common suffix/prefix, or if they are singleton.
 */
@Slf4j
@Getter
@ToString(callSuper = true)
public class StringParser extends Parser<String, StringStore, StringColumnValues> {


	private static final Pattern DIGITS = Pattern.compile("^\\d+$");

	public StringParser(ConqueryConfig config) {
		super(config, new StringColumnValues());
	}

	public void addString(String value) {
		Objects.requireNonNull(value, "value");
		recordLine();
		getColumnValues().addString(value);
	}

	protected StringStore decideType() {
		return decideType(getLines());
	}

	@Override
	protected StringStore decideType(int storeLines) {

		//check if a singleton type is enough
		if (getLines() == 0) {
			return EmptyStore.INSTANCE;
		}

		return StringStoreString.create(storeLines);
	}




	@Override
	public void setValue(StringStore store, int event, String value) {
		store.setString(event, value);
	}

}
