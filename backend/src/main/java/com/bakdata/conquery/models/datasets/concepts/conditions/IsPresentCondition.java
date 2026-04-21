package com.bakdata.conquery.models.datasets.concepts.conditions;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.BOOLEAN;
import static org.jooq.impl.SQLDataType.VARCHAR;

import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionWrappingWhereCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jooq.Condition;
import org.jooq.impl.DSL;

/**
 * This condition requires that the selected Column has a value.
 */
@CPSType(id = "PRESENT", base = CTCondition.class)
public class IsPresentCondition implements CTCondition {

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
		Condition condition = field(name(column)).isNotNull();
		return new ConditionWrappingWhereCondition(condition);
	}

	@Override
	public ConceptConditions buildExpression(CTConditionContext context, ConceptElement<?> id) {

		FieldCondition condition = new FieldCondition(context.getFunctionProvider().isNull(field(name(column))), Set.of(inline(false)));
		return new ConceptConditions(id, Map.of(field(name("%s_is_empty".formatted(column)), BOOLEAN), condition));
	}
}
