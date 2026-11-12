package com.bakdata.conquery.sql.translate.conversion.select;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

/** Resolved state used by select SQL helpers; aliases are allocated by the caller. */
public record SelectConversionContext(
		TranslatorDialect dialect,
		SqlNameGenerator nameGenerator,
		SqlTables tables,
		SqlIdColumns ids,
		Optional<ColumnDateRange> validityDate,
		Optional<ColumnDateRange> stratificationDate,
		String alias,
		Map<String, String> conceptColumnTables,
		List<ConceptColumnSource> conceptColumnSources
) {
	public SelectConversionContext(TranslatorDialect dialect, SqlNameGenerator nameGenerator, SqlTables tables, SqlIdColumns ids,
								   Optional<ColumnDateRange> validityDate, String alias) {
		this(dialect, nameGenerator, tables, ids, validityDate, Optional.empty(), alias, Map.of(), List.of());
	}

	public SelectConversionContext(TranslatorDialect dialect, SqlNameGenerator nameGenerator, SqlTables tables, SqlIdColumns ids,
								   Optional<ColumnDateRange> validityDate, String alias, Map<String, String> conceptColumnTables) {
		this(dialect, nameGenerator, tables, ids, validityDate, Optional.empty(), alias, conceptColumnTables, List.of());
	}

	public SelectConversionContext(TranslatorDialect dialect, SqlNameGenerator nameGenerator, SqlTables tables, SqlIdColumns ids,
								   Optional<ColumnDateRange> validityDate, String alias, Map<String, String> conceptColumnTables,
								   List<ConceptColumnSource> conceptColumnSources) {
		this(dialect, nameGenerator, tables, ids, validityDate, Optional.empty(), alias, conceptColumnTables,
				conceptColumnSources);
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
