package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionType;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereConditionWrapper;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jooq.Condition;
import org.jooq.impl.DSL;

/**
 * This condition requires that the selected Column has a value.
 */
@CPSType(id="PRESENT", base=CTCondition.class)
public class IsPresentCondition implements CTCondition {

	@Getter @Setter
	@NonNull
	private String column;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		return rowMap.getValue().containsKey(column);
	}

	@Override
	public WhereCondition convertToSqlCondition(CTConditionContext context) {
		Condition condition = DSL.field(DSL.name(context.getConnectorTable().getName(), column)).isNotNull();
		return new WhereConditionWrapper(condition, ConditionType.PREPROCESSING);
	}
}
