package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

/** Resolved state used by select SQL helpers; aliases are allocated by the caller. */
public record SelectConversionContext(
		CompilerDialect dialect,
		SqlNameGenerator nameGenerator,
		SqlTables tables,
		SqlIdColumns ids,
		Optional<ColumnDateRange> validityDate,
		String alias,
		Map<String, String> conceptColumnTables,
		List<ConceptColumnSource> conceptColumnSources
) {
	public SelectConversionContext(CompilerDialect dialect, SqlNameGenerator nameGenerator, SqlTables tables, SqlIdColumns ids,
			Optional<ColumnDateRange> validityDate, String alias) {
		this(dialect, nameGenerator, tables, ids, validityDate, alias, Map.of(), List.of());
	}

	public SelectConversionContext(CompilerDialect dialect, SqlNameGenerator nameGenerator, SqlTables tables, SqlIdColumns ids,
			Optional<ColumnDateRange> validityDate, String alias, Map<String, String> conceptColumnTables) {
		this(dialect, nameGenerator, tables, ids, validityDate, alias, conceptColumnTables, List.of());
	}

	public SelectConversionContext {
		conceptColumnTables = Map.copyOf(conceptColumnTables);
		conceptColumnSources = List.copyOf(conceptColumnSources);
	}

	/** SQL-resolved source for a concept-value column whose physical table needs preparation before selection. */
	public record ConceptColumnSource(
			String qualifier,
			TableLike<? extends Record> table,
			Field<?> value,
			List<Condition> conditions
	) {
		public ConceptColumnSource {
			Objects.requireNonNull(qualifier, "qualifier");
			Objects.requireNonNull(table, "table");
			Objects.requireNonNull(value, "value");
			conditions = List.copyOf(conditions);
		}
	}
}
