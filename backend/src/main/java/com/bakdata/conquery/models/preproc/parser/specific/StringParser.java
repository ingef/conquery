package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.regex.Pattern;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.stores.primitive.StringStoreString;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Analyze all strings for common suffix/prefix, or if they are singleton.
 */
@Slf4j
@Getter
@ToString(callSuper = true, of = {"encoding", "prefix", "suffix"})
public class StringParser extends Parser<String, StringStore> {


	private static final Pattern DIGITS = Pattern.compile("^\\d+$");

	private Object2IntMap<String> strings = new Object2IntOpenHashMap<>();


	public StringParser(ConqueryConfig config) {
		super(config);
	}

	/**
	 * It's either exactly `0`, or a string of digits, not starting with `0`, and no leading +-.
	 */
	public static boolean isOnlyDigits(String value) {
		if (value.startsWith("0")) {
			return value.length() == 1;
		}

		return DIGITS.matcher(value).matches();
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
		if (strings.isEmpty()) {
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
