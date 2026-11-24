package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.Map;
import jakarta.validation.Valid;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.Setter;
import org.jooq.Condition;

/**
 * This condition matches if its child does not.
 */
@CPSType(id="NOT", base=CTCondition.class)
@Deprecated
public class NotCondition implements CTCondition {

	@Setter @Getter @Valid
	private CTCondition condition;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		return !condition.matches(value, rowMap);
	}

	@Override
	public void init(ConceptElement<?> node) throws ConceptConfigurationException {
		condition.init(node);
	}

	@Override
	public Condition convertToSqlCondition(CTConditionContext context) {
		return condition.convertNegatedToSqlCondition(context);
	}

	@Override
	public ConceptConditions buildExpression(CTConditionContext context, ConceptElement<?> id) {
		throw new IllegalStateException("Not implemented");
	}
}
