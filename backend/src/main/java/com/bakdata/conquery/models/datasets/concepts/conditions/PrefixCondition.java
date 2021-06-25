package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * This condition requires each value to start with one of the given values.
 */
@CPSType(id="PREFIX_LIST", base=CTCondition.class)
@ToString
public class PrefixCondition implements CTCondition {

	@Setter @Getter @NotEmpty
	private String[] prefixes;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		for(String p:prefixes) {
			if(value.startsWith(p)) {
				return true;
			}
		}
		return false;
	}

	
}
