package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This condition requires that the selected Column has a value.
 */
@CPSType(id="PRESENT", base=CTCondition.class)
public class IsPresentCondition implements CTCondition {

	@Getter @Setter
	@NonNull
	private String column;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		return rowMap.getValue().containsKey(column);
	}
}