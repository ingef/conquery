package com.bakdata.conquery.models.concepts.conditions;

import java.util.HashSet;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * This condition requires each value to be exactly as given in the list.
 */
@CPSType(id="EQUAL", base=CTCondition.class)
public class EqualCondition implements CTCondition {

	// TODO: 06.08.2020 FK: @JsonCreator that uses different Sets for different applications. eg Collections.singleton or ArraySet (for small sets)

	@Setter @Getter @NotEmpty
	private HashSet<String> values;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		return values.contains(value);
	}
}