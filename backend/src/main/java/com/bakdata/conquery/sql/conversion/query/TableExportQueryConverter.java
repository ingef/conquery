package com.bakdata.conquery.sql.conversion.query;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.FormCteStep;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class TableExportQueryConverter implements NodeConverter<TableExportQuery> {

	private final QueryStepTransformer queryStepTransformer;

	@Override
	public Class<? extends TableExportQuery> getConversionClass() {
		return TableExportQuery.class;
	}

	@Override
	public ConversionContext convert(TableExportQuery tableExportQuery, ConversionContext context) {

		QueryStep convertedPrerequisite = convertPrerequisite(tableExportQuery, context);
		Map<ColumnId, Integer> positions = tableExportQuery.getPositions();
		CDateRange dateRestriction = CDateRange.of(tableExportQuery.getDateRange());

		List<QueryStep> convertedTables = tableExportQuery.getTables().stream()
														  .flatMap(concept -> concept.getTables().stream().map(table -> convertTable(
																  table,
																  concept,
																  dateRestriction,
																  convertedPrerequisite,
																  positions,
																  context
														  )))
														  .toList();

		QueryStep unionedTables = QueryStep.createUnionAllStep(
				convertedTables,
				null, // no CTE name required as this step will be the final select
				List.of(convertedPrerequisite)
		);
		Select<Record> selectQuery = queryStepTransformer.toSelectQuery(unionedTables);

		return context.withFinalQuery(new SqlQuery(selectQuery, tableExportQuery.getResultInfos()));
	}

	/**
	 * Converts the {@link Query} of the given {@link TableExportQuery} and creates another {@link QueryStep} on top which extracts only the primary id.
	 */
	private static QueryStep convertPrerequisite(TableExportQuery exportQuery, ConversionContext context) {

		ConversionContext withConvertedPrerequisite = context.getNodeConversions().convert(exportQuery.getQuery(), context);
		Preconditions.checkArgument(withConvertedPrerequisite.getQuerySteps().size() == 1, "Base query conversion should produce exactly 1 QueryStep");
		QueryStep convertedPrerequisite = withConvertedPrerequisite.getLastConvertedStep();

		Selects prerequisiteSelects = convertedPrerequisite.getQualifiedSelects();
		Selects selects = Selects.builder()
								 .ids(new SqlIdColumns(prerequisiteSelects.getIds().getPrimaryColumn()))
								 .build();

		return QueryStep.builder()
						.cteName(FormCteStep.EXTRACT_IDS.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(convertedPrerequisite.getCteName()))
						.groupBy(selects.getIds().toFields()) // group by primary column to ensure max. 1 entry per subject
						.predecessors(List.of(convertedPrerequisite))
						.build();
	}

	/**
	 * Create a CTE selecting all positions of a {@link TableExportQuery} from the given {@link CQTable} properly ordered. For all columns, which are not
	 * linked to the given table, we just do a null select.
	 */
	private static QueryStep convertTable(
			CQTable cqTable,
			CQConcept concept,
			CDateRange dateRestriction,
			QueryStep convertedPrerequisite,
			Map<ColumnId, Integer> positions,
			ConversionContext context
	) {
		Field<Object> primaryColumn = TablePrimaryColumnUtil.findPrimaryColumn(cqTable.getConnector().resolve().getResolvedTable(), context.getConfig());
		SqlIdColumns ids = new SqlIdColumns(primaryColumn);
    String conceptConnectorName = context.getNameGenerator().conceptConnectorName(concept, cqTable.getConnector().resolve(), context.getSqlPrintSettings().getLocale());
		Optional<ColumnDateRange> validityDate = convertTablesValidityDate(cqTable, conceptConnectorName, context);

		List<FieldWrapper<?>> exportColumns = initializeFields(cqTable, positions);
		Selects selects = Selects.builder()
								 .ids(ids)
								 .validityDate(validityDate)
								 .sqlSelects(exportColumns)
								 .build();

		List<Condition> filters = cqTable.getFilters().stream().map(filterValue -> filterValue.convertForTableExport(ids, context)).toList();
		Table<Record> joinedTable = joinConnectorTableWithPrerequisite(cqTable, ids, convertedPrerequisite, dateRestriction, context);

		return QueryStep.builder()
						.cteName(conceptConnectorName)
						.selects(selects)
						.fromTable(joinedTable)
						.conditions(filters)
						.build();
	}

	private static Optional<ColumnDateRange> convertTablesValidityDate(CQTable table, String alias, ConversionContext context) {
		if (table.findValidityDate() == null) {
			return Optional.of(ColumnDateRange.empty());
		}
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		ColumnDateRange validityDate = functionProvider.forValidityDate(table.findValidityDate());
		// when exporting tables, we want the validity date as a single-column daterange string expression straightaway
		Field<String> asStringExpression = functionProvider.encloseInCurlyBraces(functionProvider.daterangeStringExpression(validityDate));
		return Optional.of(ColumnDateRange.of(asStringExpression).asValidityDateRange(alias));
	}

	private static List<FieldWrapper<?>> initializeFields(CQTable cqTable, Map<ColumnId, Integer> positions) {

		Field<?>[] exportColumns = createPlaceholders(positions, cqTable);
		for (Column column : cqTable.getConnector().resolve().getResolvedTable().getColumns()) {
			// e.g. date column(s) are handled separately and not part of positions
			if (!positions.containsKey(column.getId())) {
				continue;
			}
			int position = positions.get(column.getId()) - 1;
			exportColumns[position] = createColumnSelect(column, position);
		}

		return Arrays.stream(exportColumns).map(FieldWrapper::new).collect(Collectors.toList());
	}

	private static Field<?>[] createPlaceholders(Map<ColumnId, Integer> positions, CQTable cqTable) {

		Field<?>[] exportColumns = new Field[positions.size() + 1];
		exportColumns[0] = createSourceInfoSelect(cqTable);

		// if columns have the same computed position, they can share a common name because they will be unioned over multiple tables anyway
		positions.forEach((column, position) -> {
			int shifted = position - 1;
			Field<?> columnSelect = DSL.inline(null, Object.class).as("%s-%d".formatted(column.getColumn(), shifted));
			exportColumns[shifted] = columnSelect;
		});

		return exportColumns;
	}

	private static Field<String> createSourceInfoSelect(CQTable cqTable) {
		String tableName = cqTable.getConnector().resolve().getResolvedTableId().getTable();
		return DSL.val(tableName).as(SharedAliases.SOURCE.getAlias());
	}

	private static Field<?> createColumnSelect(Column column, int position) {
		String columnName = "%s-%s".formatted(column.getName(), position);
		return DSL.field(DSL.name(column.getTable().getName(), column.getName()))
				  .as(columnName);
	}

	private static Table<Record> joinConnectorTableWithPrerequisite(
			CQTable cqTable,
			SqlIdColumns ids,
			QueryStep convertedPrerequisite,
			CDateRange dateRestriction,
			ConversionContext context
	) {
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		Table<Record> connectorTable = DSL.table(DSL.name(cqTable.getConnector().resolve().getResolvedTableId().getTable()));
		Table<Record> convertedPrerequisiteTable = DSL.table(DSL.name(convertedPrerequisite.getCteName()));

		ColumnDateRange validityDate = functionProvider.forValidityDate(cqTable.findValidityDate());
		List<Condition> joinConditions = Stream.concat(
				ids.join(convertedPrerequisite.getQualifiedSelects().getIds()).stream(),
				Stream.of(functionProvider.dateRestriction(functionProvider.forCDateRange(dateRestriction), validityDate))
		).toList();

		return functionProvider.innerJoin(connectorTable, convertedPrerequisiteTable, joinConditions);
	}

}
