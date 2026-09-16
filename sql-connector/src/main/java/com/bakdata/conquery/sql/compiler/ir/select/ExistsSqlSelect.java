package com.bakdata.conquery.sql.compiler.ir.select;

import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.name;

import java.util.List;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import org.jooq.Field;
import org.jooq.Name;

/** A single-column marker select indicating whether a row exists. */
public class ExistsSqlSelect implements SingleColumnSqlSelect {

	private static final Field<Integer> EXISTS = inline(1);

	private final Field<Integer> exists;
	private final Name alias;

	private ExistsSqlSelect(Field<Integer> exists, Name alias) {
		this.exists = exists;
		this.alias = alias;
	}

	public static ExistsSqlSelect withAlias(String alias) {
		return new ExistsSqlSelect(EXISTS.as(alias), name(alias));
	}

	@Override
	public List<String> requiredColumns() {
		return List.of();
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
	public SingleColumnSqlSelect qualify(String qualifier) {
		Field<Integer> qualified = field(name(name(qualifier), alias), exists.getType());
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
	public List<Field<?>> aggregateForFinalQuery(CompilerDialect dialect) {
		// Full outer joins can introduce null values, which an existence marker represents as zero.
		Field<Integer> coalesced = coalesce(max(select()), inline(0));
		return List.of(coalesced.as(alias));
	}

	private Field<Integer> coalesceWithZero() {
		return coalesce(select(), inline(0));
	}
}
