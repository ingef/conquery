package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.NumberMapUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.SumCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SingleColumnSqlSelect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Conversion of a {@link SumSelect} by summing the {@link SumSelect#getColumn()} or, if present, the {@link SumSelect#getColumn()} minus the
 * {@link SumSelect#getSubtractColumn()}.
 * <p>
 * Conversion of a {@link SumSelect} with {@link SumSelect#getDistinctByColumn()} is a special case: Sum's the values of a column for each row which is distinct
 * by the distinct-by columns by creating 2 additional CTEs. We can't use our usual {@link ConceptCteStep#PREPROCESSING} CTE for achieving distinctness, because
 * it's used for the conversion of other selects where distinctness by distinct-by columns is not required and would cause wrong results.
 *
 * <pre>
 *  The two additional CTEs this aggregator creates
 * 	<ol>
 * 	    <li>
 * 	        Assign a row number to each row partitioned by the distinct by columns to ensure distinctness.
 *            {@code
 * 	        	"row_number_assigned" as (
 *   			  select
 *   			    "pid",
 *   			    "value",
 *   			    row_number() over (partition by "pid", "k1", "k2") "row_number"
 *   			  from "event_filter"
 *   			)
 *            }
 * 	    </li>
 * 	    <li>
 * 	        Sum all entries of a subject where the row number = 1, thus only summing distinct entries.
 *            {@code
 * 	        "sum_distinct_select-1-row_number_filtered" as (
 *   		  select
 *   		    "pid",
 *   		    sum("value") "sum_distinct_select-1"
 *   		  from "row_number_assigned"
 *   		  where "row_number" = 1
 *   		  group by "pid"
 *   		),
 *            }
 * 	    </li>
 * 	</ol>
 * </pre>
 */
public class SumSqlAggregator<RANGE extends IRange<? extends Number, ?>> implements
		SelectConverter<SumSelect>,
		FilterConverter<SumFilter<RANGE>, RANGE>,
		SqlAggregator {

	@Getter
	@RequiredArgsConstructor
	private enum SumDistinctCteStep implements CteStep {

		ROW_NUMBER_ASSIGNED("row_number_assigned", null),
		ROW_NUMBER_FILTERED("row_number_filtered", ROW_NUMBER_ASSIGNED);

		private final String suffix;
		private final SumDistinctCteStep predecessor;
	}

	private static final String ROW_NUMBER_ALIAS = "row_number";
	private static final String SUM_DISTINCT_SUFFIX = "sum_distinct";

	@Override
	public ConnectorSqlSelects connectorSelect(SumSelect sumSelect, SelectContext<ConnectorSqlTables> selectContext) {

		Column sumColumn = sumSelect.getColumn();
		Column subtractColumn = sumSelect.getSubtractColumn();
		List<Column> distinctByColumns = sumSelect.getDistinctByColumn();
		NameGenerator nameGenerator = selectContext.getNameGenerator();
		String alias = nameGenerator.selectName(sumSelect);
		ConnectorSqlTables tables = selectContext.getTables();

		CommonAggregationSelect<BigDecimal> sumAggregationSelect;
		if (distinctByColumns != null && !distinctByColumns.isEmpty()) {
			SqlIdColumns ids = selectContext.getIds();
			sumAggregationSelect = createDistinctSumAggregationSelect(sumColumn, distinctByColumns, alias, ids, tables, nameGenerator);
			ExtractingSqlSelect<BigDecimal> finalSelect = createFinalSelect(sumAggregationSelect, tables);
			return ConnectorSqlSelects.builder()
									  .preprocessingSelects(sumAggregationSelect.getRootSelects())
									  .additionalPredecessor(sumAggregationSelect.getAdditionalPredecessor())
									  .finalSelect(finalSelect)
									  .build();
		}
		else {
			sumAggregationSelect = createSumAggregationSelect(sumColumn, subtractColumn, alias, tables);
			ExtractingSqlSelect<BigDecimal> finalSelect = createFinalSelect(sumAggregationSelect, tables);
			return ConnectorSqlSelects.builder()
									  .preprocessingSelects(sumAggregationSelect.getRootSelects())
									  .aggregationSelect(sumAggregationSelect.getGroupBy())
									  .finalSelect(finalSelect)
									  .build();
		}
	}

	@Override
	public SqlFilters convertToSqlFilter(SumFilter<RANGE> sumFilter, FilterContext<RANGE> filterContext) {

		Column sumColumn = sumFilter.getColumn();
		Column subtractColumn = sumFilter.getSubtractColumn();
		List<Column> distinctByColumns = sumFilter.getDistinctByColumn();
		String alias = filterContext.getNameGenerator().selectName(sumFilter);
		ConnectorSqlTables tables = filterContext.getTables();

		CommonAggregationSelect<BigDecimal> sumAggregationSelect;
		ConnectorSqlSelects selects;

		if (distinctByColumns != null && !distinctByColumns.isEmpty()) {
			sumAggregationSelect =
					createDistinctSumAggregationSelect(sumColumn, distinctByColumns, alias, filterContext.getIds(), tables, filterContext.getNameGenerator());
			selects = ConnectorSqlSelects.builder()
										 .preprocessingSelects(sumAggregationSelect.getRootSelects())
										 .additionalPredecessor(sumAggregationSelect.getAdditionalPredecessor())
										 .build();
		}
		else {
			sumAggregationSelect = createSumAggregationSelect(sumColumn, subtractColumn, alias, tables);
			selects = ConnectorSqlSelects.builder()
										 .preprocessingSelects(sumAggregationSelect.getRootSelects())
										 .additionalPredecessor(sumAggregationSelect.getAdditionalPredecessor())
										 .aggregationSelect(sumAggregationSelect.getGroupBy())
										 .build();
		}

		Field<BigDecimal> qualifiedSumSelect = sumAggregationSelect.getGroupBy().qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER)).select();
		SumCondition sumCondition = new SumCondition(qualifiedSumSelect, filterContext.getValue());
		WhereClauses whereClauses = WhereClauses.builder()
												.groupFilter(sumCondition)
												.build();

		return new SqlFilters(selects, whereClauses);

	}

	@Override
	public Condition convertForTableExport(SumFilter<RANGE> filter, FilterContext<RANGE> filterContext) {

		Column column = filter.getColumn();
		String tableName = column.getTable().getName();
		String columnName = column.getName();
		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(column.getType());
		Field<? extends Number> field = DSL.field(DSL.name(tableName, columnName), numberClass);

		Column subtractColumn = filter.getSubtractColumn();
		if (subtractColumn == null) {
			return new SumCondition(field, filterContext.getValue()).condition();
		}

		String subtractColumnName = subtractColumn.getName();
		String subtractTableName = subtractColumn.getTable().getName();
		Field<? extends Number> subtractField = DSL.field(DSL.name(subtractTableName, subtractColumnName), numberClass);
		return new SumCondition(field.minus(subtractField), filterContext.getValue()).condition();
	}

	private CommonAggregationSelect<BigDecimal> createSumAggregationSelect(Column sumColumn, Column subtractColumn, String alias, ConnectorSqlTables tables) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumColumn.getType());
		List<ExtractingSqlSelect<?>> preprocessingSelects = new ArrayList<>();

		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), sumColumn.getName(), numberClass);
		preprocessingSelects.add(rootSelect);

		String eventFilterCte = tables.cteName(ConceptCteStep.EVENT_FILTER);
		Field<? extends Number> sumField = rootSelect.qualify(eventFilterCte).select();

		FieldWrapper<BigDecimal> sumGroupBy;

		if (subtractColumn != null) {
			ExtractingSqlSelect<? extends Number> subtractColumnRootSelect = new ExtractingSqlSelect<>(
					tables.getRootTable(),
					subtractColumn.getName(),
					numberClass
			);
			preprocessingSelects.add(subtractColumnRootSelect);

			Field<? extends Number> subtractField = subtractColumnRootSelect.qualify(eventFilterCte).select();
			sumGroupBy = new FieldWrapper<>(DSL.sum(sumField.minus(subtractField)).as(alias), sumColumn.getName(), subtractColumn.getName());
		}
		else {
			sumGroupBy = new FieldWrapper<>(DSL.sum(sumField).as(alias), sumColumn.getName());
		}

		return CommonAggregationSelect.<BigDecimal>builder()
									  .rootSelects(preprocessingSelects)
									  .groupBy(sumGroupBy)
									  .build();
	}

	private CommonAggregationSelect<BigDecimal> createDistinctSumAggregationSelect(
			Column sumColumn,
			List<Column> distinctByColumns,
			String alias,
			SqlIdColumns ids,
			ConnectorSqlTables tables,
			NameGenerator nameGenerator
	) {
		List<ExtractingSqlSelect<?>> preprocessingSelects = new ArrayList<>();

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumColumn.getType());
		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), sumColumn.getName(), numberClass);
		preprocessingSelects.add(rootSelect);

		List<ExtractingSqlSelect<?>> distinctByRootSelects =
				distinctByColumns.stream()
								 .map(column -> new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), Object.class))
								 .collect(Collectors.toList());
		preprocessingSelects.addAll(distinctByRootSelects);

		QueryStep rowNumberCte = createRowNumberCte(ids, rootSelect, distinctByRootSelects, alias, tables, nameGenerator);
		Field<? extends Number> rootSelectQualified = rootSelect.qualify(rowNumberCte.getCteName()).select();
		FieldWrapper<BigDecimal> sumGroupBy = new FieldWrapper<>(DSL.sum(rootSelectQualified).as(alias));
		QueryStep rowNumberFilteredCte = createRowNumberFilteredCte(rowNumberCte, sumGroupBy, alias, nameGenerator);

		return CommonAggregationSelect.<BigDecimal>builder()
									  .rootSelects(preprocessingSelects)
									  .additionalPredecessor(rowNumberFilteredCte)
									  .groupBy(sumGroupBy)
									  .build();
	}

	/**
	 * Assigns row numbers for each partition over the pid and the distinct by columns. If the values per pid in the distinct by columns are duplicated,
	 * the row number will be incremented for each duplicated entry.
	 */
	private static QueryStep createRowNumberCte(
			SqlIdColumns ids,
			SingleColumnSqlSelect sumColumnRootSelect,
			List<ExtractingSqlSelect<?>> distinctByRootSelects,
			String alias,
			SqlTables connectorTables,
			NameGenerator nameGenerator
	) {
		String predecessor = connectorTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT);
		SqlIdColumns qualifiedIds = ids.qualify(predecessor);
		SingleColumnSqlSelect qualifiedSumRootSelect = sumColumnRootSelect.qualify(predecessor);

		List<Field<?>> partitioningFields = Stream.concat(
														  qualifiedIds.toFields().stream(),
														  distinctByRootSelects.stream().map(sqlSelect -> sqlSelect.qualify(predecessor).select())
												  )
												  .collect(Collectors.toList());
		FieldWrapper<Integer> rowNumber = new FieldWrapper<>(
				DSL.rowNumber().over(DSL.partitionBy(partitioningFields)).as(ROW_NUMBER_ALIAS),
				partitioningFields.stream().map(Field::getName).toArray(String[]::new)
		);

		Selects rowNumberAssignedSelects = Selects.builder()
												  .ids(qualifiedIds)
												  .sqlSelects(List.of(qualifiedSumRootSelect, rowNumber))
												  .build();

		return QueryStep.builder()
						.cteName(nameGenerator.cteStepName(SumDistinctCteStep.ROW_NUMBER_ASSIGNED, alias))
						.selects(rowNumberAssignedSelects)
						.fromTable(QueryStep.toTableLike(predecessor))
						.build();
	}

	/**
	 * Sums up the sum column values but only those whose row number is 1. Thus, only unique entries will be summed up.
	 */
	private static QueryStep createRowNumberFilteredCte(
			QueryStep rowNumberCte,
			FieldWrapper<BigDecimal> sumSelect,
			String alias,
			NameGenerator nameGenerator
	) {
		SqlIdColumns ids = rowNumberCte.getQualifiedSelects().getIds();

		Selects rowNumberFilteredSelects = Selects.builder()
												  .ids(ids)
												  .sqlSelects(List.of(sumSelect))
												  .build();

		Condition firstOccurrence = DSL.field(DSL.name(rowNumberCte.getCteName(), ROW_NUMBER_ALIAS))
									   .eq(DSL.val(1));

		return QueryStep.builder()
						.cteName(nameGenerator.cteStepName(SumDistinctCteStep.ROW_NUMBER_FILTERED, alias))
						.selects(rowNumberFilteredSelects)
						.fromTable(QueryStep.toTableLike(rowNumberCte.getCteName()))
						.conditions(List.of(firstOccurrence))
						.predecessors(List.of(rowNumberCte))
						.groupBy(ids.toFields())
						.build();
	}

	private static ExtractingSqlSelect<BigDecimal> createFinalSelect(CommonAggregationSelect<BigDecimal> sumAggregationSelect, ConnectorSqlTables tables) {
		String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		return sumAggregationSelect.getGroupBy().qualify(finalPredecessor);
	}

}
