package com.bakdata.conquery.models.concepts.conditions;

import java.util.Arrays;
import java.util.Collection;
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

	@Override
	public boolean covers(Collection<CTCondition> childConditions) {
		condition_loop:
		for (CTCondition childCondition : childConditions) {
			if (childCondition instanceof PrefixCondition) {
				// All child prefixes must be covered by any of this conditions prefixes
				PrefixCondition condition = (PrefixCondition) childCondition;
				for (String childPrefix : condition.getPrefixes()) {
					if(Arrays.stream(prefixes).anyMatch(prefix -> prefix.startsWith(childPrefix))){
						continue;
					}
					return false;
				}
			}
			else if (childCondition instanceof PrefixRangeCondition) {
				PrefixRangeCondition condition = (PrefixRangeCondition) childCondition;
				for (String prefix : prefixes) {
					// Min and Max must be covered by a single prefix not multiple
					if(condition.getMin().startsWith(prefix) && condition.getMax().startsWith(prefix)) {
						// Go on with the next condition
						break condition_loop;
					}
				}
				return false;
			}
			else if (childCondition instanceof EqualCondition) {
				EqualCondition condition = (EqualCondition) childCondition;
				for (String value : condition.getValues()) {
					if (Arrays.stream(prefixes).anyMatch(prefixes -> value.startsWith(prefixes))){
						continue;
					}
					return false;
				}
			}
		}
		return true;
	}
}
