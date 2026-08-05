package com.bakdata.conquery.models.datasets.concepts.conditions;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.SQLDataType.VARCHAR;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Sets;
import org.jooq.Field;
import org.jooq.Name;
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

	ConceptConditions buildExpression(CTConditionContext context, ConceptElement<?> id);

	/**
	 * Extractor is used to join original data onto join-Table.
	 *
	 * @param extractor field statement to extract value from source table
	 * @param params values result of extractor is compared against.
	 */
	record FieldCondition(Field<?> extractor, Set<Param<?>> params) {

	}

	/**
	 * Describes mapping of connector fields by way of {@link FieldCondition} to specific conceptElements.
	 *
	 * @param conceptElement The conceptElement being defined by the conditions
	 * @param conditions The conditions defining the conceptElement. Fields are assumed to be and-ed, multiple entries in a field are or-ed.
	 *                   So a definition of `{"a": [1], "b": [1,2]}` emits the rows [{a=1 AND b=1}, {a=1  AND b=2}].
	 *
	 *
	 */
	record ConceptConditions(ConceptElement<?> conceptElement, Map<Field<?>, FieldCondition> conditions) {
		public ConceptConditions and(ConceptConditions other) {
			if (other == null) {
				return this;
			}

			Set<Name> fields = Stream.of(other.conditions.keySet(), conditions.keySet()).flatMap(Collection::stream)
									 .map(Field::getUnqualifiedName)
									 .collect(Collectors.toSet());

			Map<Name, Field<?>> rByName = other.conditions.keySet().stream().collect(Collectors.toMap(Field::getUnqualifiedName, Function.identity()));
			Map<Name, Field<?>> lByName = conditions.keySet().stream().collect(Collectors.toMap(Field::getUnqualifiedName, Function.identity()));

			Map<Field<?>, FieldCondition> combined = new HashMap<>();

			// AND combine fields, if both are present.
			for (Name fieldName : fields) {
				Field<?> rField = rByName.get(fieldName);
				Field<?> lField = lByName.get(fieldName);

				if (rField == null) {
					combined.put(lField, conditions.get(lField));
					continue;
				}

				if (lField == null) {
					combined.put(rField, other.conditions.get(rField));
					continue;
				}

				// If both are present, intersect params
				FieldCondition otherCond = other.conditions.get(rField);
				FieldCondition myCond = conditions.get(lField);

				FieldCondition condition = new FieldCondition(myCond.extractor, Sets.intersection(otherCond.params, myCond.params));

				// Recompute string field to fit in all values.
				// Otherwise, assume both fields are the same type
				Field<?> outField;
				if (lField.getDataType().isString()) {
					outField = field(lField.getUnqualifiedName(), VARCHAR(Math.max(lField.getDataType().length(), rField.getDataType().length())));
				}
				else {
					assert lField.getDataType().equals(rField.getDataType());
					outField = lField;
				}
				combined.put(outField, condition);
			}

			return new ConceptConditions(conceptElement(), combined);
		}
	}


}
