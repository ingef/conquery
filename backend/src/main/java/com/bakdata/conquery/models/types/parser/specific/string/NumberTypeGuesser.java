package com.bakdata.conquery.models.types.parser.specific.string;

import java.util.Arrays;
import java.util.Map.Entry;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.IntegerRange;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.specific.IntegerParser;
import com.bakdata.conquery.models.types.specific.string.StringTypeNumber;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NumberTypeGuesser implements TypeGuesser {

	private final StringParser p;

	@Override
	public Guess createGuess() {
		//check if the remaining strings are all numbers
		try {
			Range<Integer> range = new IntegerRange(0, 0);
			IntegerParser numberParser = new IntegerParser();
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

			CType<Long> decision = numberParser.findBestType();
			p.copyLineCounts(decision);

			final StringTypeNumber type = new StringTypeNumber(range, decision, p.getStrings().inverse());

			return new Guess(
					this,
					type,
					decision.estimateMemoryConsumption(),
					0
			);
		}
		catch (NumberFormatException e) {
			return null;
		}


	}

}
