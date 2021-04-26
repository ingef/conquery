package com.bakdata.conquery.models.concepts.conditions;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.tree.Prefix;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This condition requires each value to start with a prefix between the two given values
 */
@CPSType(id = "PREFIX_RANGE", base = ConceptTreeCondition.class)
@RequiredArgsConstructor
public class PrefixRangeCondition implements ConceptTreeCondition {

	@Getter
	@NotEmpty
	private final String min;
	@Getter
	@NotEmpty
	private final String max;

	@ValidationMethod(message = "Min and max need to be of the same length and min needs to be smaller than max.")
	@JsonIgnore
	public boolean isValidMinMax() {
		if (min.length() != max.length()) {
			return false;
		}
		return min.compareTo(max) < 0;
	}


	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		if (value.length() >= min.length()) {
			String pref = value.substring(0, min.length());
			return min.compareTo(pref) <= 0 && max.compareTo(pref) >= 0;
		}
		return false;
	}

	@Override
	public Map<String, RangeSet<Prefix>> getColumnSpan() {
		return Map.of(ConceptTreeCondition.COLUMN_PLACEHOLDER, ImmutableRangeSet.of(Range.closed(Prefix.of(min), Prefix.of(max))));
	}
}
