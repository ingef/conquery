package com.bakdata.conquery.sql.conversion.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
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
import com.google.common.base.Predicate;
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
		Map<Column, Integer>
				positions =
				tableExportQuery.getPositions().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().resolve(), Map.Entry::getValue));
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

		QueryStep unionedTables = QueryStep.createUnionStep(
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
			Map<Column, Integer> positions,
			ConversionContext context
	) {
		Field<Object> primaryColumn = TablePrimaryColumnUtil.findPrimaryColumn(cqTable.getConnector().resolve().getResolvedTable(), context.getConfig());
		SqlIdColumns ids = new SqlIdColumns(primaryColumn);
		String conceptConnectorName = context.getNameGenerator().conceptConnectorName(concept, cqTable.getConnector().resolve());
		Optional<ColumnDateRange> validityDate = convertTablesValidityDate(cqTable, conceptConnectorName, dateRestriction, context);

		List<Field<?>> exportColumns = new ArrayList<>();
		exportColumns.add(createSourceInfoSelect(cqTable));

		positions.entrySet().stream()
				 .sorted(Comparator.comparingInt(Map.Entry::getValue))
				 .map(entry -> createColumnSelect(cqTable, entry))
				 .forEach(exportColumns::add);

		Selects selects = Selects.builder()
								 .ids(ids)
								 .validityDate(validityDate)
								 .sqlSelects(exportColumns.stream().map(FieldWrapper::new).collect(Collectors.toList()))
								 .build();

		List<Condition> filters = cqTable.getFilters().stream().map(filterValue -> filterValue.convertForTableExport(ids, context)).toList();
		Table<Record> joinedTable = joinConnectorTableWithPrerequisite(cqTable, ids, convertedPrerequisite, context);

		return QueryStep.builder()
						.cteName(conceptConnectorName)
						.selects(selects)
						.fromTable(joinedTable)
						.conditions(filters)
						.build();
	}

	private static Optional<ColumnDateRange> convertTablesValidityDate(CQTable table, String alias, CDateRange dateRestriction, ConversionContext context) {
		if (table.findValidityDate() == null) {
			return Optional.of(ColumnDateRange.empty());
		}
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		ColumnDateRange validityDate = functionProvider.forValidityDate(table.findValidityDate(), dateRestriction);
		// when exporting tables, we want the validity date as a single-column daterange string expression straightaway
		Field<String> asStringExpression = functionProvider.encloseInCurlyBraces(functionProvider.daterangeStringExpression(validityDate));
		return Optional.of(ColumnDateRange.of(asStringExpression).asValidityDateRange(alias));
	}

	private static Field<String> createSourceInfoSelect(CQTable cqTable) {
		String tableName = cqTable.getConnector().resolve().getResolvedTable().getName();
		return DSL.val(tableName).as(SharedAliases.SOURCE.getAlias());
	}

	private static Field<?> createColumnSelect(CQTable table, Map.Entry<Column, Integer> entry) {

		Column column = entry.getKey();
		Integer columnPosition = entry.getValue();
		String columnName = "%s-%s".formatted(column.getName(), columnPosition);

		if (!isColumnOfTable(column, table)) {
			return DSL.inline(null, Object.class).as(columnName);
		}
		return DSL.field(DSL.name(column.getTable().getName(), column.getName()))
				  .as(columnName);
	}

	private static boolean isColumnOfTable(Column column, CQTable table) {
		return columnIsConnectorColumn(column, table)
			   || columnIsSecondaryIdOfConnectorTable(column, table)
			   || columnIsConnectorTableColumn(column, table);
	}

	private static boolean columnIsConnectorTableColumn(Column column, CQTable table) {
		return matchesTableColumnOn(table, tableColumn -> tableColumn == column);
	}

	private static boolean columnIsSecondaryIdOfConnectorTable(Column column, CQTable table) {
		return column.getSecondaryId() != null && matchesTableColumnOn(table, tableColumn -> tableColumn.getSecondaryId() == column.getSecondaryId());
	}

	private static boolean matchesTableColumnOn(CQTable table, Predicate<Column> condition) {
		return Arrays.stream(table.getConnector().resolve().getResolvedTable().getColumns()).anyMatch(condition);
	}

	private static boolean columnIsConnectorColumn(Column column, CQTable table) {
		return table.getConnector().resolve().getColumn() != null && table.getConnector().resolve().getColumn().resolve().equals(column);
	}

	private static Table<Record> joinConnectorTableWithPrerequisite(
			CQTable cqTable,
			SqlIdColumns ids,
			QueryStep convertedPrerequisite,
			ConversionContext context
	) {
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		Table<Record> connectorTable = DSL.table(DSL.name(cqTable.getConnector().resolve().getResolvedTable().getName()));
		List<Condition> joinOnIds = ids.join(convertedPrerequisite.getQualifiedSelects().getIds());
		return functionProvider.innerJoin(connectorTable, convertedPrerequisite, joinOnIds);
	}

}
