package com.bakdata.conquery.models.datasets.concepts.conditions;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.SQLDataType.VARCHAR;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionWrappingWhereCondition;
import com.bakdata.conquery.util.CalculatedValue;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jooq.Condition;

/**
 * This condition requires each value to start with one of the given values.
 */
@CPSType(id = "PREFIX_LIST", base = CTCondition.class)
@ToString
@Deprecated
public class PrefixCondition implements CTCondition {

	@Setter
	@Getter
	@NotEmpty
	private String[] prefixes;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		for (String p : prefixes) {
			if (value.startsWith(p)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public WhereCondition convertToSqlCondition(CTConditionContext context) {
		String pattern = Arrays.stream(prefixes).collect(Collectors.joining("|", "", context.getFunctionProvider().getAnyCharRegex()));
		Condition condition = context.getFunctionProvider().likeRegex(field(context.getConnectorColumn(), VARCHAR), pattern);
		return new ConditionWrappingWhereCondition(condition);
	}

	@Override
	public ConceptConditions buildExpression(CTConditionContext context, ConceptElement<?> id) {
		// Implementation is technically possible but extremely slow and PREFIX has caused issues historically
		throw new IllegalStateException("Not implemented");
	}
}
