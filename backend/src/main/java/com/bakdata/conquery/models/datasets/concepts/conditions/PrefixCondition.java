package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.Arrays;
import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionType;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereConditionWrapper;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jooq.Field;
import org.jooq.LikeEscapeStep;
import org.jooq.impl.DSL;

/**
 * This condition requires each value to start with one of the given values.
 */
@CPSType(id = "PREFIX_LIST", base = CTCondition.class)
@ToString
public class PrefixCondition implements CTCondition {

	@Setter
	@Getter
	@NotEmpty
	private String[] prefixes;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		for (String p : prefixes) {
			if (value.startsWith(p)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public WhereCondition convertToSqlCondition(CTConditionContext context) {
		Field<String> field = DSL.field(DSL.name(context.getConnectorTable().getName(), context.getConnectorColumn().getName()), String.class);
		LikeEscapeStep condition = Arrays.stream(prefixes)
										 .map(prefix -> field.like(prefix + "%"))
										 .reduce((like1, like2) -> (LikeEscapeStep) like1.or(like2))
										 .orElseThrow(() -> new IllegalStateException(
												 "At least 1 prefix is required for the conversion of %s to a SQL where clause.".formatted(getClass()))
										 );
		return new WhereConditionWrapper(condition, ConditionType.PREPROCESSING);
	}
}
