package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.Setter;
import org.jooq.Condition;
import org.jooq.impl.DSL;

/**
 * This condition connects multiple conditions with an or.
 */
@CPSType(id = "OR", base = CTCondition.class)
public class OrCondition implements CTCondition {

	@Setter
	@Getter
	@Valid
	@NotEmpty
	private List<CTCondition> conditions;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		for (CTCondition cond : conditions) {
			if (cond.matches(value, rowMap)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void init(ConceptElement<?> node) throws ConceptConfigurationException {
		for (CTCondition cond : conditions) {
			cond.init(node);
		}
	}

	@Override
	public Condition convertToSqlCondition(CTConditionContext context) {
		return DSL.or(conditions.stream()
								 .map(condition -> condition.convertToSqlCondition(context)).toList());

	}
}
