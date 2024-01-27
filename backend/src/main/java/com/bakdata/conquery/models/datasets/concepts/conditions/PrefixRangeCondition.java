package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionType;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereConditionWrapper;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * This condition requires each value to start with a prefix between the two given values
 */
@CPSType(id="PREFIX_RANGE", base=CTCondition.class)
public class PrefixRangeCondition implements CTCondition {

	private static final String ANY_CHAR_REGEX = ".*";

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
	public WhereCondition convertToSqlCondition(CTConditionContext context) {
		Field<String> field = DSL.field(DSL.name(context.getConnectorTable().getName(), context.getConnectorColumn().getName()), String.class);
		String pattern = buildSqlRegexPattern();
		Condition regexCondition = context.getFunctionProvider().likeRegex(field, pattern);
		return new WhereConditionWrapper(regexCondition, ConditionType.PREPROCESSING);
	}

	private String buildSqlRegexPattern() {
		StringBuilder builder = new StringBuilder();
		char[] minChars = min.toCharArray();
		char[] maxChars = max.toCharArray();
		for (int i = 0; i < minChars.length; i++) {
			char minChar = minChars[i];
			char maxChar = maxChars[i];
			if (minChar != maxChar) {
				builder.append("[%s-%s]".formatted(minChar, maxChar));
			}
			else {
				builder.append(minChar);
			}
		}
		return builder.append(ANY_CHAR_REGEX).toString();
	}
}
