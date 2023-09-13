package com.bakdata.conquery.sql.conversion.cqelement.concept.model.select;

import java.util.List;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelect;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
@Builder
@EqualsAndHashCode
public class FirstValueSqlSelect implements SqlSelect {

	Field<?> firstColumn;
	String alias;
	List<Field<?>> orderByColumns;
	@EqualsAndHashCode.Exclude
	SqlFunctionProvider functionProvider;

	@Override
	public Field<?> select() {
		return functionProvider.first(firstColumn, orderByColumns)
							   .as(alias);
	}

	@Override
	public Field<?> aliased() {
		return DSL.field(alias);
	}

	@Override
	public String columnName() {
		return firstColumn.getName();
	}

}
