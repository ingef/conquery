package com.bakdata.conquery.models.concepts.conditions;

import java.util.*;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Ordering;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;

/**
 * This condition requires each value to start with a prefix between the two given values
 */
@CPSType(id="PREFIX_RANGE", base=CTCondition.class)
public class PrefixRangeCondition implements CTCondition {

	@Getter @Setter @NotEmpty
	private String min;
	@Getter @Setter @NotEmpty
	private String max;
	
	@ValidationMethod(message="Min and max need to be of the same length and min needs to be smaller than max.") @JsonIgnore
	public boolean isValidMinMax() {
		if(min.length()!=max.length()) {
			return false;
		}
		return min.compareTo(max)<0;
	}


	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		if(value.length()>=min.length()) {
			String pref = value.substring(0,min.length());
			return min.compareTo(pref)<=0 && max.compareTo(pref)>=0;
		}
		return false;
	}


	@Override
	public boolean covers(Collection<CTCondition> childConditions) {

		for (CTCondition childCondition : childConditions) {
			if (childCondition instanceof PrefixCondition) {
				// All child prefixes must be in range
				PrefixCondition condition = (PrefixCondition) childCondition;
				for (String childPrefix : condition.getPrefixes()) {
					if(	   (childPrefix.startsWith(min) || Ordering.natural().isOrdered(List.of(min, childPrefix)))
						&& (childPrefix.startsWith(max) || Ordering.natural().isOrdered(List.of(childPrefix, max))) ) {
						continue;
					}
					return false;
				}
			}
			else if (childCondition instanceof PrefixRangeCondition) {
				PrefixRangeCondition condition = (PrefixRangeCondition) childCondition;
				if(	   (condition.getMin().startsWith(min) || Ordering.natural().isOrdered(List.of(min, condition.getMin())))
					&& (condition.getMax().startsWith(max) || Ordering.natural().isOrdered(List.of(condition.getMax(), max))) ) {
					// Go on with the next condition
					continue;
				}
				return false;
			}
			else if (childCondition instanceof EqualCondition) {
				EqualCondition condition = (EqualCondition) childCondition;
				for (String value : condition.getValues()) {
					if(	   (value.startsWith(min) || Ordering.natural().isOrdered(List.of(min, value)))
						&& (value.startsWith(max) || Ordering.natural().isOrdered(List.of(value, max))) ) {
						continue;
					}
					return false;
				}
			}
		}
		return true;
	}
}
