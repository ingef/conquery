package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jooq.Field;
import org.jooq.Param;

/**
 * A general condition that serves as a guard for concept tree nodes.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface CTCondition {

	default void init(ConceptElement<?> node) throws ConceptConfigurationException {
	}

	boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException;

	WhereCondition convertToSqlCondition(CTConditionContext context);

	Set<String> auxiliaryColumns();

	Expression expressions(CTConditionContext context, ConceptElement<?> id);


	record Expression(ConceptElement<?> id, Map<Field<?>, Set<Param<?>>> conditions) {
		public Expression join(Expression other) {
			// We are overwriting their conditions!
			Map<Field<?>, Set<Param<?>>> combined = new HashMap<>(other.conditions());
			combined.putAll(conditions());
			return new Expression(id(), combined);
		}
	}


}
