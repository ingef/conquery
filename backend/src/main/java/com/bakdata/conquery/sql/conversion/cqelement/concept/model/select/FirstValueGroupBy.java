package com.bakdata.conquery.sql.conversion.cqelement.concept.model.select;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FirstValueGroupBy extends ConquerySelect {

	private final Field<?> field;
	private final List<Field<Object>> validityDateColumns;
	private final SqlFunctionProvider functionProvider;

	@Override
	public Field<?> select() {
		List<Field<?>> qualifiedValidityDates = validityDateColumns.stream()
																   .map(validityDate -> DSL.field(DSL.name(getQualifier(), validityDate.getName())))
																   .collect(Collectors.toList());
		return functionProvider.first(DSL.name(getQualifier(), field.getName()), qualifiedValidityDates)
							   .as(field.getName());
	}

	@Override
	public Field<?> alias() {
		return DSL.field(field.getName());
	}

}
