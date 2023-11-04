package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class PrefixSqlSelect implements SqlSelect {

	private final Field<String> columnToPrefix;
	private final String prefix;
	private final SqlFunctionProvider functionProvider;
	private final String alias;

	@Override
	public Field<String> select() {
		return functionProvider.prefixStringAggregation(columnToPrefix, prefix)
							   .as(alias);
	}
	
	@Override
	public Field<?> aliased() {
		return DSL.field(alias);
	}

	@Override
	public String columnName() {
		return columnToPrefix.getName();
	}

}
