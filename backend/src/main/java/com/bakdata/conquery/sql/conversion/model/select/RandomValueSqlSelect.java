package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
@Builder
@EqualsAndHashCode
public class RandomValueSqlSelect implements SqlSelect {

	Field<?> randomColumn;
	String alias;
	@EqualsAndHashCode.Exclude
	SqlFunctionProvider functionProvider;

	@Override
	public Field<?> select() {
		return functionProvider.random(randomColumn).as(alias);
	}

	@Override
	public Field<?> aliased() {
		return DSL.field(alias);
	}

	@Override
	public String columnName() {
		return randomColumn.getName();
	}

}
