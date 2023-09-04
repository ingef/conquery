package com.bakdata.conquery.sql.conversion.cqelement.concept.model.select;

import java.util.List;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FirstValueGroupBy implements ConquerySelect {

	private final Field<?> field;
	private final List<Field<?>> validityDateColumns;
	private final SqlFunctionProvider functionProvider;

	@Override
	public Field<?> select() {
		return functionProvider.first(field, validityDateColumns)
							   .as(field.getName());
	}

	@Override
	public Field<?> alias() {
		return DSL.field(field.getName());
	}

}
