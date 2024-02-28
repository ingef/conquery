package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.MultiSelectCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.CollectionsUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * This condition requires each value to be exactly as given in the list.
 */
@CPSType(id="EQUAL", base=CTCondition.class)
@AllArgsConstructor
public class EqualCondition implements CTCondition {

	@Setter @Getter @NotEmpty
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
		Field<String> field = DSL.field(DSL.name(context.getConnectorTable().getName(), context.getConnectorColumn().getName()), String.class);
		return new MultiSelectCondition(field, values.toArray(String[]::new), context.getFunctionProvider());
	}
}
