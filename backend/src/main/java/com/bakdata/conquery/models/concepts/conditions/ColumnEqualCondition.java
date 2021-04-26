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
import lombok.Setter;

/**
 * This condition requires the value of another column to be equal to a given value.
 */
@CPSType(id = "COLUMN_EQUAL", base = ConceptTreeCondition.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator(mode = JsonCreator.Mode.PROPERTIES))
public class ColumnEqualCondition implements ConceptTreeCondition {

	@Setter
	@Getter
	@NotEmpty
	private final Set<String> values;
	@NotEmpty
	@Setter
	@Getter
	private final String column;


	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		Object checkedValue = rowMap.getValue().get(column);
		if (checkedValue == null) {
			return false;
		}
		return values.contains(checkedValue.toString());
	}

	@Override
	public Map<String, RangeSet<Prefix>> getColumnSpan() {
		final RangeSet<Prefix> rangeSet = TreeRangeSet.create();
		for (String value : values) {
			rangeSet.add(Range.singleton(Prefix.of(value)));
		}

		return Map.of(getColumn(), rangeSet);
	}
}