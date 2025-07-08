package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExistsSqlSelect implements SingleColumnSqlSelect {

	private static final Field<Integer> EXISTS = DSL.val(1);

	private final Field<Integer> exists;
	private final Name alias;

	public static ExistsSqlSelect withAlias(final String alias) {
		return new ExistsSqlSelect(EXISTS.as(alias), DSL.name(alias));
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
		return this.exists;
	}

	@Override
	public Field<Integer> aliased() {
		return DSL.field(exists.getName(), exists.getType());
	}

	@Override
	public SingleColumnSqlSelect qualify(final String qualifier) {
		final Field<Integer> qualified = DSL.field(DSL.name(DSL.name(qualifier), alias), exists.getType());
		return new ExistsSqlSelect(qualified, alias);
	}

	@Override
	public SqlSelect connectorAggregate() {
		return new ExistsSqlSelect(DSL.max(coalesceWithZero()).as(alias), alias);
	}

	@Override
	public SqlSelect toFinalRepresentation() {
		return new ExistsSqlSelect(coalesceWithZero().as(alias), alias);
	}

	private Field<Integer> coalesceWithZero() {
		return DSL.coalesce(select(), DSL.value(0));
	}
}
