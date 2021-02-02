package com.bakdata.conquery.models.events.parser.specific.string;

import java.util.Arrays;
import java.util.Map.Entry;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.IntegerRange;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.parser.specific.IntegerParser;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeNumber;
import lombok.RequiredArgsConstructor;

/**
 * Guess, testing if all values can be represented as Integer Numbers without leading zeros. If selected, the values will be compressed using {@link IntegerParser}.
 */
@RequiredArgsConstructor
public class NumberTypeGuesser extends StringTypeGuesser {

	private final StringParser p;
	private final ParserConfig config;

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

			ColumnStore<Long> decision = numberParser.findBestType();
			p.copyLineCounts(decision);

			final StringTypeNumber type = new StringTypeNumber(range, decision, p.getStrings().inverse());

			return new Guess(
					type,
					decision.estimateMemoryConsumptionBytes(),
					0
			);
		}
		catch (NumberFormatException e) {
			return null;
		}


	}

}
