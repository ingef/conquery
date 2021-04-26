package com.bakdata.conquery.models.concepts.conditions;

import java.util.Collections;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.tree.Prefix;
import com.bakdata.conquery.util.CalculatedValue;
import com.google.common.collect.RangeSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This condition requires that the selected Column has a value.
 */
@CPSType(id = "PRESENT", base = ConceptTreeCondition.class)
public class IsPresentCondition implements ConceptTreeCondition {

	@Getter
	@Setter
	@NonNull
	private String column;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		return rowMap.getValue().containsKey(column);
	}

	@Override
	public Map<String, RangeSet<Prefix>> getColumnSpan() {
		return Collections.emptyMap();
	}
}