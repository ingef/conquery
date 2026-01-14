package com.bakdata.conquery.models.datasets.concepts.conditions;

import static org.jooq.impl.DSL.*;

import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionWrappingWhereCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jooq.Condition;

/**
 * This condition requires that the selected Column has a value.
 */
@CPSType(id = "NOT_PRESENT", base = CTCondition.class)
public class IsEmptyCondition implements CTCondition {

	@Getter
	@Setter
	@NonNull
	private String column;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		return rowMap.getValue().containsKey(column);
	}

	@Override
	public WhereCondition convertToSqlCondition(CTConditionContext context) {
		Condition condition = context.access(column).isNull();
		return new ConditionWrappingWhereCondition(condition);
	}

	@Override
	public Set<String> auxiliaryColumns() {
		return Set.of(column);
	}

	@Override
	public Expression expressions(CTConditionContext context, ConceptElementId<?> id) {
		return new Expression(id, Map.of(context.access(column).isNull(), Set.of(val(true))));
	}
}
