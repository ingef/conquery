package com.bakdata.conquery.models.datasets.concepts.conditions;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.SQLDataType.VARCHAR;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.MultiSelectCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.CollectionsUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jooq.impl.DSL;

/**
 * This condition requires each value to be exactly as given in the list.
 */
@CPSType(id = "EQUAL", base = CTCondition.class)
@AllArgsConstructor
public class EqualCondition implements CTCondition {

	@Setter
	@Getter
	@NotEmpty
	private Set<String> values;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public static EqualCondition create(Set<String> values) {
		return new EqualCondition(CollectionsUtil.createSmallestSet(values));
	}

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		return values.contains(value);
	}

	@Override
	public WhereCondition convertToSqlCondition(CTConditionContext context) {
		return new MultiSelectCondition(field(context.getConnectorColumn(), VARCHAR), values.toArray(String[]::new), context.getFunctionProvider());
	}

	private int fieldLength() {
		return values.stream().mapToInt(String::length).max().orElse(0);
	}

	@Override
	public Expression buildExpression(CTConditionContext context, ConceptElement<?> id) {
		return new Expression(id, Map.of(field(context.getConnectorColumn(), VARCHAR(fieldLength())), values.stream().map(DSL::val).collect(Collectors.toSet())));
	}
}
