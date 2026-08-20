package com.bakdata.conquery.sql.conversion.model.select;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

import java.util.Collections;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.field;

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

	private Field<Integer> coalesceWithZero() {
		return coalesce(select(), value(0));
	}
}
