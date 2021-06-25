package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.CollectionsUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * This condition requires the value of another column to be equal to a given value.
 */
@CPSType(id="COLUMN_EQUAL", base=CTCondition.class)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ColumnEqualCondition implements CTCondition {

	@Setter @Getter @NotEmpty
	private Set<String> values;
	@NotEmpty @Setter @Getter
	private String column;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public static ColumnEqualCondition create(Set<String> values, String column) {
		return new ColumnEqualCondition(CollectionsUtil.createSmallestSet(values), column);
	}

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		Object checkedValue = rowMap.getValue().get(column);
		if(checkedValue == null) {
			return false;
		}
		return values.contains(checkedValue.toString());
	}
}