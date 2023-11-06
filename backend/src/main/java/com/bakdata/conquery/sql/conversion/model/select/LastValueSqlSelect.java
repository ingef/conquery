package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
@Builder
@EqualsAndHashCode
public class LastValueSqlSelect implements SqlSelect {

	Field<?> lastColumn;
	String alias;
	List<Field<?>> orderByColumns;
	@EqualsAndHashCode.Exclude
	SqlFunctionProvider functionProvider;

	@Override
	public Field<?> select() {
		return functionProvider.last(lastColumn, orderByColumns)
							   .as(alias);
	}

	@Override
	public Field<?> aliased() {
		return DSL.field(alias);
	}

	@Override
	public List<String> columnNames() {
		return List.of(lastColumn.getName());
	}

}
