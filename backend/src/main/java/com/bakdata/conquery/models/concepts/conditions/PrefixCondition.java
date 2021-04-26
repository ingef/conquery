package com.bakdata.conquery.models.concepts.conditions;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.tree.Prefix;
import com.bakdata.conquery.util.CalculatedValue;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * This condition requires each value to start with one of the given values.
 */
@CPSType(id = "PREFIX_LIST", base = ConceptTreeCondition.class)
@ToString
@RequiredArgsConstructor
public class PrefixCondition implements ConceptTreeCondition {

	@Getter
	@NotEmpty
	private final String[] prefixes;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		for (String p : prefixes) {
			if (value.startsWith(p)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Map<String, RangeSet<Prefix>> getColumnSpan() {
		final RangeSet<Prefix> rangeSet = TreeRangeSet.create();
		for (String value : prefixes) {
			rangeSet.add(Range.singleton(Prefix.of(value)));
		}

		return Map.of(ConceptTreeCondition.COLUMN_PLACEHOLDER, rangeSet);
	}


}
