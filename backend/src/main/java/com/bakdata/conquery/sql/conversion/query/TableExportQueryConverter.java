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

	/**
	 * Validity date is part of positions, but not when converting to SQL because it might have multiple columns and not just one.
	 * Thus, we need to apply an offset to the positions.
	 */
	private static final int POSITION_OFFSET = 1;

	private final QueryStepTransformer queryStepTransformer;

	@Override
	public Class<? extends TableExportQuery> getConversionClass() {
		return TableExportQuery.class;
	}

	@Override
	public ConversionContext convert(TableExportQuery tableExportQuery, ConversionContext context) {

		final QueryStep convertedPrerequisite = convertPrerequisite(tableExportQuery, context);
		final Map<ColumnId, Integer> positions = tableExportQuery.getPositions();
		final CDateRange dateRestriction = CDateRange.of(tableExportQuery.getDateRange());

		final List<QueryStep> convertedTables = tableExportQuery.getTables().stream()
																.flatMap(concept -> concept.getTables().stream().map(table -> convertTable(
																		table,
																		concept,
																		dateRestriction,
																		convertedPrerequisite,
																		positions,
																		context
																)))
																.toList();

		final QueryStep unionedTables = QueryStep.createUnionAllStep(
				convertedTables,
				null, // no CTE name required as this step will be the final select
				List.of(convertedPrerequisite)
		);
		final Select<Record> selectQuery = queryStepTransformer.toSelectQuery(unionedTables);

		return context.withFinalQuery(new SqlQuery(selectQuery, tableExportQuery.getResultInfos()));
	}

	/**
	 * Converts the {@link Query} of the given {@link TableExportQuery} and creates another {@link QueryStep} on top which extracts only the primary id.
	 */
	private static QueryStep convertPrerequisite(TableExportQuery exportQuery, ConversionContext context) {

		final ConversionContext withConvertedPrerequisite = context.getNodeConversions().convert(exportQuery.getQuery(), context);
		Preconditions.checkArgument(withConvertedPrerequisite.getQuerySteps().size() == 1, "Base query conversion should produce exactly 1 QueryStep");
		final QueryStep convertedPrerequisite = withConvertedPrerequisite.getLastConvertedStep();

		final Selects prerequisiteSelects = convertedPrerequisite.getQualifiedSelects();
		final Selects selects = Selects.builder()
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
		final Field<Object> primaryColumn = TablePrimaryColumnUtil.findPrimaryColumn(cqTable.getConnector().resolve().getResolvedTable(), context.getConfig());
		final SqlIdColumns ids = new SqlIdColumns(primaryColumn);
		final String conceptConnectorName =
				context.getNameGenerator().conceptConnectorName(concept, cqTable.getConnector().resolve(), context.getSqlPrintSettings().getLocale());
		final Optional<ColumnDateRange> validityDate = convertTablesValidityDate(cqTable, conceptConnectorName, context);

		final List<FieldWrapper<?>> exportColumns = initializeFields(cqTable, positions);

		final Selects selects = Selects.builder()
									   .ids(ids)
									   .validityDate(validityDate)
									   .sqlSelects(exportColumns)
									   .build();

		final List<Condition> filters = cqTable.getFilters().stream().map(filterValue -> filterValue.convertForTableExport(ids, context)).toList();
		final Table<Record> joinedTable = joinConnectorTableWithPrerequisite(cqTable, ids, convertedPrerequisite, dateRestriction, context);

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
		final SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		final ColumnDateRange validityDate = functionProvider.forValidityDate(table.findValidityDate());
		// when exporting tables, we want the validity date as a single-column daterange string expression straightaway
		final Field<String> asStringExpression = functionProvider.encloseInCurlyBraces(functionProvider.daterangeStringExpression(validityDate));
		return Optional.of(ColumnDateRange.of(asStringExpression).asValidityDateRange(alias));
	}

	private static List<FieldWrapper<?>> initializeFields(CQTable cqTable, Map<ColumnId, Integer> positions) {

		final Field<?>[] exportColumns = createPlaceholders(positions);

		exportColumns[0] = createSourceInfoSelect(cqTable);

		for (Column column : cqTable.getConnector().resolve().getResolvedTable().getColumns()) {
			// e.g. date column(s) are handled separately and not part of positions
			if (!positions.containsKey(column.getId())) {
				continue;
			}
			final int position = positions.get(column.getId()) - POSITION_OFFSET;
			exportColumns[position] = createColumnSelect(column, position);
		}

		return Arrays.stream(exportColumns).map(FieldWrapper::new).collect(Collectors.toList());
	}

	private static Table<Record> joinConnectorTableWithPrerequisite(
			CQTable cqTable,
			SqlIdColumns ids,
			QueryStep convertedPrerequisite,
			CDateRange dateRestriction,
			ConversionContext context
	) {
		final SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		final Table<Record> connectorTable = DSL.table(DSL.name(cqTable.getConnector().resolve().getResolvedTableId().getTable()));
		final Table<Record> convertedPrerequisiteTable = DSL.table(DSL.name(convertedPrerequisite.getCteName()));

		final ColumnDateRange validityDate = functionProvider.forValidityDate(cqTable.findValidityDate());
		final List<Condition> joinConditions = Stream.concat(
				ids.join(convertedPrerequisite.getQualifiedSelects().getIds()).stream(),
				Stream.of(functionProvider.dateRestriction(functionProvider.forCDateRange(dateRestriction), validityDate))
		).toList();

		return functionProvider.innerJoin(connectorTable, convertedPrerequisiteTable, joinConditions);
	}

	private static Field<?>[] createPlaceholders(Map<ColumnId, Integer> positions) {

		final int size = TableExportQuery.calculateWidth(positions) - POSITION_OFFSET;
		final Field<?>[] exportColumns = new Field[size];

		// if columns have the same computed position, they can share a common name because they will be unioned over multiple tables anyway
		for (int index = 0; index < exportColumns.length; index++) {
			final Field<?> columnSelect = DSL.inline(null, Object.class).as("null-%d".formatted(index));
			exportColumns[index] = columnSelect;
		}

		return exportColumns;
	}

	private static Field<String> createSourceInfoSelect(CQTable cqTable) {
		final String tableName = cqTable.getConnector().resolve().getResolvedTableId().getTable();
		return DSL.val(tableName).as(SharedAliases.SOURCE.getAlias());
	}

	private static Field<?> createColumnSelect(Column column, int position) {
		final String columnName = "%s-%s".formatted(column.getName(), position);
		return DSL.field(DSL.name(column.getTable().getName(), column.getName()))
				  .as(columnName);
	}

}
