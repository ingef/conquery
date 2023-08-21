package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.regex.Pattern;

import com.bakdata.conquery.models.config.ConqueryConfig;
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

//	TODO
	//	public NumberStringStore tryCreateNumberStringStore(ConqueryConfig config) {
//
//		//check if the remaining strings are all numbers
//		final IntegerParser numberParser = new IntegerParser(config);
//
//		try {
//
//			for (String s : getStrings().keySet()) {
//
//				// Ensure there are only digits and no other leading zeroes.
//				if (!isOnlyDigits(s)) {
//					return null;
//				}
//
//				long parseInt = Integer.parseInt(s);
//				numberParser.addLine(parseInt);
//			}
//		}
//		catch (NumberFormatException e) {
//			return null;
//		}
//
//
//		numberParser.setLines(getLines());
//
//		/*
//		Do not use a number type if the range is much larger than the number if distinct values
//		e.g. if the column contains only 0 and 5M
//		 */
//
//		final long span = numberParser.getMaxValue() - numberParser.getMinValue() + 1;
//
//		if (span > getStrings().size()) {
//			return null;
//		}
//
//		IntegerStore decision = numberParser.findBestType();
//
//		Int2ObjectMap<String> inverse = new Int2ObjectOpenHashMap<>(getStrings().size());
//		getStrings().forEach((key, value) -> inverse.putIfAbsent((int) value, key));
//
//		return new NumberStringStore(new Range.IntegerRange((int) numberParser.getMinValue(), (int) numberParser.getMaxValue()), decision, inverse);
//	}

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
//		if (strings.isEmpty()) {
//			return EmptyStore.INSTANCE;
//		}
		//TODO StringStoreNumbers

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
