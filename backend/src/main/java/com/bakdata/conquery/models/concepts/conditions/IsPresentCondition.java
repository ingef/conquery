package com.bakdata.conquery.models.concepts.conditions;

import java.util.Collections;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.tree.Prefix;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.RangeSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This condition requires that the selected Column has a value.
 */
@CPSType(id = "PRESENT", base = ConceptTreeCondition.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class IsPresentCondition implements ConceptTreeCondition {

	@Getter
	@NonNull
	private final String column;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		return rowMap.getValue().containsKey(column);
	}

	@Override
	public Map<String, RangeSet<Prefix>> getColumnSpan() {
		return Collections.emptyMap();
	}
}