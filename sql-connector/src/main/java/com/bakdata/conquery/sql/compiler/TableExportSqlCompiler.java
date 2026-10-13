package com.bakdata.conquery.sql.compiler;

import static org.jooq.impl.DSL.inline;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.conversion.operation.ResolvedTableExportFilterConverter;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.ProjectionMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SchemaSql;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SharedAliases;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.condition.DateRestrictionCondition;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.compiler.rendering.QueryStepRenderer;
import com.bakdata.conquery.sql.model.export.ResolvedExportTable;
import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
import com.bakdata.conquery.sql.model.schema.ResolvedValidityDate;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.conf.ParamType;

/** Compiler for row-level table exports. */
public final class TableExportSqlCompiler {

	private final QueryStepRenderer renderer;
	private final ResolvedQueryStepCompiler stepCompiler = new ResolvedQueryStepCompiler();
	private final ResolvedTableExportFilterConverter filterConverter = new ResolvedTableExportFilterConverter();

	public TableExportSqlCompiler(DSLContext dslContext) {
		this.renderer = new QueryStepRenderer(dslContext);
	}

	public CompiledQuery compile(ResolvedTableExportQuery query, CompilerDialect dialect) {
		SqlNameGenerator names = new SqlNameGenerator(dialect.getNameMaxLength());
		QueryStep prerequisite = extractIds(stepCompiler.compile(
				query.prerequisite(), dialect, names, Optional.empty()).step());
		List<QueryStep> tables = query.tables().stream()
				.map(table -> compileTable(table, query, prerequisite, dialect, names)).toList();
		QueryStep union = QueryStep.createUnionAllStep(tables, null, List.of(prerequisite), false);
		Select<Record> rendered = renderer.toSelectQuery(union, dialect);
		return new CompiledQuery(rendered.getSQL(ParamType.INLINED), CompiledQueryColumns.create(
				query.prerequisite().entitySchema(), query.resultColumns(), rendered.getSelect()));
	}

	private static QueryStep extractIds(QueryStep prerequisite) {
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(prerequisite.getQualifiedSelects().getIds().getPrimaryColumn()))
				.build();
		return QueryStep.builder()
				.cteName("extract_ids")
				.selects(selects)
				.fromTable(QueryStep.toTableLike(prerequisite.getCteName()))
				.groupBy(selects.getIds().toFields())
				.predecessors(List.of(prerequisite))
				.build();
	}

	private QueryStep compileTable(ResolvedExportTable value, ResolvedTableExportQuery query,
			QueryStep prerequisite, CompilerDialect dialect, SqlNameGenerator names) {
		SqlIdColumns ids = new SqlIdColumns(SchemaSql.field(value.primaryId(), String.class));
		ColumnDateRange validity = validityDate(value.validityDate(), dialect);
		List<FieldWrapper<?>> output = new java.util.ArrayList<>();
		output.add(new FieldWrapper<>(inline(value.source()).as(SharedAliases.SOURCE.getAlias())));
		for (int index = 1; index < value.outputColumns().size(); index++) {
			int outputIndex = index;
			Optional<com.bakdata.conquery.sql.model.schema.ResolvedColumn> column = value.outputColumns().get(index);
			Field<?> field = column.<Field<?>>map(resolved -> SchemaSql.field(resolved, Object.class)
					.as(resolved.physicalName() + "-" + outputIndex))
					.orElseGet(() -> inline(null, Object.class).as("null-" + outputIndex));
			output.add(new FieldWrapper<>(field));
		}
		Selects selects = Selects.builder().ids(ids).validityDate(Optional.of(validity)).sqlSelects(output).build();
		Table<Record> source = SchemaSql.table(value.table());
		Table<Record> prerequisiteTable = org.jooq.impl.DSL.table(org.jooq.impl.DSL.name(prerequisite.getCteName()));
		List<Condition> joins = new java.util.ArrayList<>(ids.join(prerequisite.getQualifiedSelects().getIds()));
		joins.add(new DateRestrictionCondition(dialect.dateRangeLiteral(query.dateRestriction()), validity).condition());
		Table<Record> joined = source.innerJoin(prerequisiteTable).on(joins.toArray(Condition[]::new));
		return QueryStep.builder()
				.cteName(names.legacyConceptConnectorName(value.conceptName(), value.connectorName()))
				.selects(selects)
				.fromTable(joined)
				.conditions(value.filters().stream().map(filter -> filterConverter.convert(filter, dialect)).toList())
				.projectionMode(ProjectionMode.INDIVIDUAL)
				.build();
	}

	private static ColumnDateRange validityDate(ResolvedValidityDate value, CompilerDialect dialect) {
		return switch (value) {
			case ResolvedValidityDate.None ignored -> dialect.emptyDateRange();
			case ResolvedValidityDate.Point point -> {
				Field<Date> field = SchemaSql.field(point.column(), Date.class);
				yield dialect.dateRange(field, field);
			}
			case ResolvedValidityDate.Range range -> dialect.dateRange(
					SchemaSql.field(range.start(), Date.class), SchemaSql.field(range.end(), Date.class));
		};
	}
}
