package com.bakdata.conquery.sql.conversion.model.select;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.field;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Name;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExistsSqlSelect implements SingleColumnSqlSelect {

	private static final Field<Integer> EXISTS = inline(1);

	private final Field<Integer> exists;
	private final Name alias;

	public static ExistsSqlSelect withAlias(final String alias) {
		return new ExistsSqlSelect(EXISTS.as(alias), name(alias));
	}

	@Override
	public List<String> requiredColumns() {
		return Collections.emptyList();
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public Field<Integer> select() {
		return exists;
	}

	@Override
	public Field<Integer> aliased() {
		return field(exists.getName(), exists.getType());
	}

	@Override
	public SingleColumnSqlSelect qualify(final String qualifier) {
		final Field<Integer> qualified = field(name(name(qualifier), alias), exists.getType());
		return new ExistsSqlSelect(qualified, alias);
	}

	@Override
	public SqlSelect connectorAggregate() {
		return new ExistsSqlSelect(max(coalesceWithZero()).as(alias), alias);
	}

	@Override
	public SqlSelect toFinalRepresentation() {
		return new ExistsSqlSelect(coalesceWithZero().as(alias), alias);
	}

	@Override
	public List<Field<?>> aggregateForFinalQuery(SqlFunctionProvider functionProvider) {
		// We have to coalesce at the end of the query because full-outer-joins will create null values, which we want to avoid.
		Field<Integer> coalesced = coalesce(max(select()), inline(0));
		return List.of(coalesced.as(alias));
	}

	private Field<Integer> coalesceWithZero() {
		return coalesce(select(), inline(0));
	}
}
