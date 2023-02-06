package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

}
