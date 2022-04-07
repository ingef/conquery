package com.bakdata.conquery.models.preproc.parser.specific.string;

import java.util.Arrays;
import java.util.Map.Entry;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.IntegerRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeNumber;
import com.bakdata.conquery.models.preproc.parser.specific.IntegerParser;
import com.bakdata.conquery.models.preproc.parser.specific.StringParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Guess, testing if all values can be represented as Integer Numbers without leading zeros. If selected, the values will be compressed using {@link IntegerParser}.
 */
@RequiredArgsConstructor
@ToString(of = "p")
public class NumberTypeGuesser extends StringTypeGuesser {

	private final StringParser p;
	private final ConqueryConfig config;

	@Override
	public Guess createGuess() {
		//check if the remaining strings are all numbers
		try {
			Range<Integer> range = new IntegerRange(0, 0);
			IntegerParser numberParser = new IntegerParser(config);
			int[] intMap = new int[p.getStrings().size()];
			Arrays.fill(intMap, -1);
			for (Entry<String, Integer> e : p.getStrings().entrySet()) {
				int intValue = Integer.parseInt(e.getKey());
				//check that there are no leading zeroes that we would destroy
				if (e.getKey().startsWith("0") && !e.getKey().equals("0")) {
					return null;
				}
				intMap[e.getValue()] = intValue;
				range = range.span(new IntegerRange(intValue, intValue));
				numberParser.addLine((long) intValue);
			}

			numberParser.setLines(p.getLines());

			//do not use a number type if the range is much larger than the number if distinct values
			//e.g. if the column contains only 0 and 5M
			int span = range.getMax() - range.getMin() + 1;
			if (span > p.getStrings().size()) {
				return null;
			}

			IntegerStore decision = numberParser.findBestType();

			Range<Integer> finalRange = range;
			return new Guess(
					null,
					decision.estimateMemoryConsumptionBytes(),
					0
			){
				@Override
				public StringStore getType() {

					Int2ObjectMap<String> inverse = new Int2ObjectOpenHashMap<>(p.getStrings().size());
					p.getStrings().forEach((key, value) -> inverse.putIfAbsent((int) value,key));

					final StringTypeNumber type = new StringTypeNumber(finalRange, decision, inverse);

					return type;
				}
			};
		}
		catch (NumberFormatException e) {
			return null;
		}


	}

}
