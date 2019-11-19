package com.bakdata.conquery.models.concepts.conditions;

import java.util.HashSet;
import java.util.Map;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.CalculatedValue;

import lombok.Getter;
import lombok.Setter;

/**
 * This condition requires the value of another column to be equal to a given value.
 */
@CPSType(id="COLUMN_EQUAL", base=CTCondition.class)
public class ColumnEqualCondition implements CTCondition {

	@Setter @Getter @NotEmpty
	private HashSet<String> values;
	@NotEmpty @Setter @Getter
	private String column;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		Object checkedValue = rowMap.getValue().get(column);
		if(checkedValue == null) {
			return false;
		}
		return values.contains(checkedValue.toString());
	}
}