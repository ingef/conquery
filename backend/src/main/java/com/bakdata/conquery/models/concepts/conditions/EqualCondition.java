package com.bakdata.conquery.models.concepts.conditions;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.CollectionsUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * This condition requires each value to be exactly as given in the list.
 */
@CPSType(id="EQUAL", base=CTCondition.class)
@AllArgsConstructor
public class EqualCondition implements CTCondition {

	@Setter @Getter @NotEmpty
	private Set<String> values;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public static EqualCondition create(Set<String> values) {
		return new EqualCondition(CollectionsUtil.createSmallestSet(values));
	}



	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		return values.contains(value);
	}

	@Override
	public boolean covers(Collection<CTCondition> childConditions) {
		for (CTCondition childCondition : childConditions) {
			if (!(childCondition instanceof EqualCondition)) {
				// Equals condition can only contain equals eonditions
				return false;
			}
			EqualCondition condition = (EqualCondition) childCondition;

			if(Sets.union(values, condition.getValues()).size() != values.size()){
				// The child contained more children that the parent
				return false;
			}
		}
		return true;
	}
}