package com.bakdata.conquery.models.concepts.conditions;

import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.tree.Prefix;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This condition requires each value to be exactly as given in the list.
 */
@CPSType(id = "EQUAL", base = ConceptTreeCondition.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator(mode = JsonCreator.Mode.PROPERTIES))
public class EqualCondition implements ConceptTreeCondition {

	@Getter
	@NotEmpty
	private final Set<String> values;


	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		return values.contains(value);
	}

	@Override
	public Map<String, RangeSet<Prefix>> getColumnSpan() {
		final RangeSet<Prefix> rangeSet = TreeRangeSet.create();
		for (String value : values) {
			rangeSet.add(Range.singleton(Prefix.of(value)));
		}

		return Map.of(ConceptTreeCondition.COLUMN_PLACEHOLDER, rangeSet);
	}
}