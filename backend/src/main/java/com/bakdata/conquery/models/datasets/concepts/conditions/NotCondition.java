package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.Map;
import jakarta.validation.Valid;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.Setter;

/**
 * This condition matches if its child does not.
 */
@CPSType(id="NOT", base=CTCondition.class)
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
	public WhereCondition convertToSqlCondition(CTConditionContext context) {
		WhereCondition whereCondition = condition.convertToSqlCondition(context);
		return whereCondition.negate();
	}
}
