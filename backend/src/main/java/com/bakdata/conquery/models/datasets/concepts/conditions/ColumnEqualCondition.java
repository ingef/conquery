package com.bakdata.conquery.models.datasets.concepts.conditions;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * This condition requires the value of another column to be equal to a given value.
 */
@CPSType(id = "COLUMN_EQUAL", base = CTCondition.class)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ColumnEqualCondition implements CTCondition {

	@Setter
	@Getter
	@NotEmpty
	private Set<String> values;
	@NotEmpty
	@Setter
	@Getter
	private String column;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public static ColumnEqualCondition create(Set<String> values, String column) {
		return new ColumnEqualCondition(CollectionsUtil.createSmallestSet(values), column);
	}

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		Object checkedValue = rowMap.getValue().get(column);
		if (checkedValue == null) {
			return false;
		}
		return values.contains(checkedValue.toString());
	}

	@Override
	public WhereCondition convertToSqlCondition(CTConditionContext context) {
		Field<String> field = field(name(column), String.class);
		return new MultiSelectCondition(field, values.toArray(String[]::new), context.getFunctionProvider());
	}

	private int fieldLength() {
		return values.stream().mapToInt(String::length).max().orElse(0);
	}

	@Override
	public ConceptConditions buildExpression(CTConditionContext context, ConceptElement<?> id) {
		FieldCondition condition = new FieldCondition(field(name(getColumn()), VARCHAR), values.stream().map(DSL::val).collect(Collectors.toSet()));

		return new ConceptConditions(id, Map.of(field(name("%s_equal".formatted(column)), VARCHAR(fieldLength())), condition));
	}
}
