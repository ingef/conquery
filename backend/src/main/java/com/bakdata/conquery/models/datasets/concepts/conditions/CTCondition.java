package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Sets;
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

	//TODO implement using join-table
	WhereCondition convertToSqlCondition(CTConditionContext context);

	Expression buildExpression(CTConditionContext context, ConceptElement<?> id);


	/**
	 * @param conceptElement The conceptElement being defined by the conditions
	 * @param conditions The conditions defining the conceptElement. Fields are assumed to be and-ed, multiple entries in a field are or-ed.
	 *                   So a definition of `{"a": [1], "b": [1,2]}` emits the rows [{a=1 AND b=1}, {a=1  AND b=2}].
	 *
	 */
	//TODO better name
	record Expression(ConceptElement<?> conceptElement, Map<Field<?>, Set<Param<?>>> conditions) {
		public Expression and(Expression other) {
			if (other == null) {
				return this;
			}

			Set<Field<?>> fields = new HashSet<>();
			fields.addAll(other.conditions.keySet());
			fields.addAll(conditions.keySet());

			Map<Field<?>, Set<Param<?>>> combined = new HashMap<>(conditions().size() + other.conditions().size());

			// AND combine fields, if both are present.
			for (Field<?> field : fields) {
				Set<Param<?>> otherParams = other.conditions.get(field);
				Set<Param<?>> myParams = conditions.get(field);

				Set<Param<?>> fieldParams;

				if (otherParams == null || otherParams.isEmpty()) {
					fieldParams = myParams;
				}
				else if (myParams == null || myParams.isEmpty()) {
					fieldParams = otherParams;
				}
				else {
					fieldParams = Sets.union(otherParams, myParams);
				}

				combined.put(field, fieldParams);
			}

			return new Expression(conceptElement(), combined);
		}
	}


}
