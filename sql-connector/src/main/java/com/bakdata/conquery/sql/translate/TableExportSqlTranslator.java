package com.bakdata.conquery.sql.translate;

import static org.jooq.impl.DSL.inline;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.export.ResolvedTableExportFilterConverter;
import com.bakdata.conquery.sql.translate.ir.ProjectionMode;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SchemaSql;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SharedAliases;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.condition.DateRestrictionCondition;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.translate.rendering.QueryStepRenderer;
import com.bakdata.conquery.sql.model.export.ResolvedExportTable;
import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.ResolvedValidityDate;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.conf.ParamType;

/** Compiler for row-level table exports. */
public final class TableExportSqlTranslator {

	private final QueryStepRenderer renderer;

	public TableExportSqlTranslator(DSLContext dslContext) {
		this.renderer = new QueryStepRenderer(dslContext);
	}

	public TranslatedQuery translate(ResolvedTableExportQuery query, TranslatorDialect dialect) {
		SqlNameGenerator names = new SqlNameGenerator(dialect.getNameMaxLength());
		QueryStep union = translateStep(query, dialect, names, new ResolvedQuerySourceStepTranslator(), false);
		Select<Record> rendered = renderer.toSelectQuery(union, dialect);
		return new TranslatedQuery(rendered.getSQL(ParamType.INLINED), TranslatedColumnUtil.create(
				query.prerequisite().entitySchema(), query.resultColumns(), rendered.getSelect()));
	}

	static QueryStep translateStep(
			ResolvedTableExportQuery query,
			TranslatorDialect dialect,
			SqlNameGenerator names,
			ResolvedQuerySourceStepTranslator sourceTranslator,
			boolean reusable
	) {
		QueryStep prerequisite = extractIds(
				sourceTranslator.translate(query.prerequisite(), dialect, names), names);
		ResolvedTableExportFilterConverter filterConverter = new ResolvedTableExportFilterConverter();
		List<QueryStep> tables = query.tables().stream()
				.map(table -> translateTable(
						table, query, prerequisite, dialect, names, filterConverter, reusable)).toList();
		return QueryStep.createUnionAllStep(
				tables, names.reusableStepName("table_export_result"), List.of(prerequisite), false);
	}

	private static QueryStep extractIds(QueryStep prerequisite, SqlNameGenerator names) {
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(prerequisite.getQualifiedSelects().getIds().getPrimaryColumn()))
				.build();
		return QueryStep.builder()
				.cteName(names.reusableStepName("extract_ids"))
				.selects(selects)
				.fromTable(QueryStep.toTableLike(prerequisite.getCteName()))
				.groupBy(selects.getIds().toFields())
				.predecessors(List.of(prerequisite))
				.build();
	}

	private static QueryStep translateTable(ResolvedExportTable value, ResolvedTableExportQuery query,
									 QueryStep prerequisite, TranslatorDialect dialect, SqlNameGenerator names,
									 ResolvedTableExportFilterConverter filterConverter, boolean reusable) {
		SqlIdColumns ids = new SqlIdColumns(SchemaSql.field(value.primaryId(), String.class));
		ColumnDateRange validity = validityDate(value.validityDate(), dialect);
		String cteName = names.legacyConceptConnectorName(value.conceptName(), value.connectorName());
		List<FieldWrapper<?>> output = new java.util.ArrayList<>();
		output.add(new FieldWrapper<>(inline(value.source()).as(SharedAliases.SOURCE.getAlias())));
		for (int index = 1; index < value.outputColumns().size(); index++) {
			int outputIndex = index;
			Optional<ResolvedColumn> column = value.outputColumns().get(index);
			Field<?> field = column.<Field<?>>map(resolved -> SchemaSql.field(resolved, Object.class)
					.as(resolved.physicalName() + "-" + outputIndex))
					.orElseGet(() -> inline(null, Object.class).as("null-" + outputIndex));
			output.add(new FieldWrapper<>(field));
		}
		Selects selects = reusable
				? Selects.builder()
					.ids(ids)
					.validityDate(Optional.of(validity.asValidityDateRange(cteName)))
					.build()
				: Selects.builder().ids(ids).validityDate(Optional.of(validity)).sqlSelects(output).build();
		Table<Record> source = SchemaSql.table(value.table());
		Table<Record> prerequisiteTable = org.jooq.impl.DSL.table(org.jooq.impl.DSL.name(prerequisite.getCteName()));
		List<Condition> joins = new java.util.ArrayList<>(ids.join(prerequisite.getQualifiedSelects().getIds()));
		joins.add(new DateRestrictionCondition(dialect.dateRangeLiteral(query.dateRestriction()), validity).condition());
		Table<Record> joined = source.innerJoin(prerequisiteTable).on(joins.toArray(Condition[]::new));
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(joined)
				.conditions(value.filters().stream().map(filter -> filterConverter.convert(filter, dialect)).toList())
				.projectionMode(reusable ? ProjectionMode.INTERMEDIATE : ProjectionMode.INDIVIDUAL)
				.build();
	}

	private static ColumnDateRange validityDate(ResolvedValidityDate value, TranslatorDialect dialect) {
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
