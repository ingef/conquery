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
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.NumberMapUtil;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
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
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Conversion of a {@link SumSelect} by summing the {@link SumSelect#getColumn()} or, if present, the {@link SumSelect#getColumn()} minus the
 * {@link SumSelect#getSubtractColumn()}.
 * <p>
 * Conversion of a {@link SumSelect} with {@link SumSelect#getDistinctByColumn()} assigns a row number in the
 * {@link ConceptCteStep#PREPROCESSING} CTE. The row number is partitioned by the entity IDs and distinct-by columns. The aggregation then only sums values
 * from the first row in each partition.
 *
 * <pre>
 *  The two stages used for a distinct sum
 * 	<ol>
 * 	    <li>
 * 	        Assign a row number during preprocessing to each row partitioned by the entity IDs and distinct-by columns.
 *            {@code
 * 	        	"preprocessing" as (
 *   			  select
 *   			    "pid",
 *   			    "value",
 *   			    row_number() over (partition by "pid", "k1", "k2") "sum_distinct_select-1"
 *   			  from "table"
 *   			)
 *            }
 * 	    </li>
 * 	    <li>
 * 	        Conditionally sum the first row from each partition in the regular aggregation CTE.
 *            {@code
 * 	        "group_select" as (
 *   		  select
 *   		    "pid",
 *   		    sum(case when "sum_distinct_select-1" = 1 then coalesce("value", 0) end) "sum_distinct_select-1"
 *   		  from "preprocessing"
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

	@Override
	public ConnectorSqlSelects connectorSelect(SumSelect sumSelect, SelectContext<ConnectorSqlTables> selectContext) {

		String alias = selectContext.getNameGenerator().selectName(sumSelect);

		Column sumColumn = sumSelect.getColumn().resolve();
		Column subtractColumn = sumSelect.getSubtractColumn() != null ? sumSelect.getSubtractColumn().resolve() : null;

		List<Column> distinctByColumns = sumSelect.getDistinctByColumn().stream().map(ColumnId::resolve).toList();

		ConnectorSqlTables tables = selectContext.getTables();

		CommonAggregationSelect<BigDecimal> sumAggregationSelect;

		if (!distinctByColumns.isEmpty()) {
			SqlIdColumns ids = selectContext.getIds();
			sumAggregationSelect = createDistinctSumAggregationSelect(sumColumn, distinctByColumns, alias, ids, tables);
			ExtractingSqlSelect<BigDecimal> finalSelect = createFinalSelect(sumAggregationSelect, tables);
			return ConnectorSqlSelects.builder()
									  .preprocessingSelects(sumAggregationSelect.getRootSelects())
									  .aggregationSelect(sumAggregationSelect.getGroupBy())
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

	private CommonAggregationSelect<BigDecimal> createDistinctSumAggregationSelect(
			Column sumColumn,
			List<Column> distinctByColumns,
			String alias,
			SqlIdColumns ids,
			ConnectorSqlTables tables
	) {
		List<SingleColumnSqlSelect> preprocessingSelects = new ArrayList<>();

		Class<? extends Number> numberClass = NumberMapUtil.getType(sumColumn);
		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), sumColumn.getName(), numberClass);
		preprocessingSelects.add(rootSelect);

		List<ExtractingSqlSelect<?>> distinctByRootSelects =
				distinctByColumns.stream()
								 .map(column -> new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), Object.class))
								 .collect(Collectors.toList());
		preprocessingSelects.addAll(distinctByRootSelects);

		List<Field<?>> partitioningFields = Stream.concat(
				ids.toFields().stream(),
				distinctByRootSelects.stream().map(ExtractingSqlSelect::select)
		).collect(Collectors.toList());
		FieldWrapper<Integer> rowNumber = new FieldWrapper<>(
				DSL.rowNumber().over(DSL.partitionBy(partitioningFields)).as(alias),
				partitioningFields.stream().map(Field::getName).toArray(String[]::new)
		);
		preprocessingSelects.add(rowNumber);

		String preprocessingCte = tables.cteName(ConceptCteStep.PREPROCESSING);
		Field<? extends Number> qualifiedRootSelect = rootSelect.qualify(preprocessingCte).select();
		Field<Integer> qualifiedRowNumber = rowNumber.qualify(preprocessingCte).select();
		Field<? extends Number> sumValue = DSL.coalesce(qualifiedRootSelect, DSL.inline(0));
		Field<? extends Number> distinctSumValue = DSL.when(qualifiedRowNumber.eq(DSL.inline(1)), sumValue);
		FieldWrapper<BigDecimal> sumGroupBy = new FieldWrapper<>(DSL.sum(distinctSumValue).as(alias));

		return CommonAggregationSelect.<BigDecimal>builder()
									  .rootSelects(preprocessingSelects)
									  .groupBy(sumGroupBy)
									  .build();
	}

	private static ExtractingSqlSelect<BigDecimal> createFinalSelect(CommonAggregationSelect<BigDecimal> sumAggregationSelect, ConnectorSqlTables tables) {
		String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		return sumAggregationSelect.getGroupBy().qualify(finalPredecessor);
	}

	private CommonAggregationSelect<BigDecimal> createSumAggregationSelect(Column sumColumn, Column subtractColumn, String alias, ConnectorSqlTables tables) {

		Class<? extends Number> numberClass = NumberMapUtil.getType(sumColumn);
		List<ExtractingSqlSelect<?>> preprocessingSelects = new ArrayList<>();

		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), sumColumn.getName(), numberClass);
		preprocessingSelects.add(rootSelect);

		String preprocessingCte = tables.cteName(ConceptCteStep.PREPROCESSING);
		Field<? extends Number> sumField = rootSelect.qualify(preprocessingCte).select();

		FieldWrapper<BigDecimal> sumGroupBy;


		if (subtractColumn != null) {
			ExtractingSqlSelect<? extends Number> subtractColumnRootSelect = new ExtractingSqlSelect<>(
					tables.getRootTable(),
					subtractColumn.getName(),
					numberClass
			);
			preprocessingSelects.add(subtractColumnRootSelect);

			Field<? extends Number> subtractField = subtractColumnRootSelect.qualify(preprocessingCte).select();


			// This expression ensures that if there's any non-null field, we get a 0. But if there's only nulls we get a null:
			// COALESCE would always result in 0 which is undesired to differentiate between missing-values and 0 sums.
			Field<? extends Number> zeroIfAnyNonNull = DSL.coalesce(sumField.multiply(0), subtractField.multiply(0));

			sumGroupBy = new FieldWrapper<>(DSL.sum(DSL.coalesce(sumField, zeroIfAnyNonNull).minus(DSL.coalesce(subtractField, zeroIfAnyNonNull))).as(alias),
											sumColumn.getName(),
											subtractColumn.getName()
			);
		}
		else {
			sumGroupBy = new FieldWrapper<>(DSL.sum(sumField).as(alias), sumColumn.getName());
		}

		return CommonAggregationSelect.<BigDecimal>builder()
									  .rootSelects(preprocessingSelects)
									  .groupBy(sumGroupBy)
									  .build();
	}

	@Override
	public SqlFilters convertToSqlFilter(SumFilter<RANGE> sumFilter, FilterContext<RANGE> filterContext) {

		Column sumColumn = sumFilter.getColumn().resolve();
		Column subtractColumn = sumFilter.getSubtractColumn() != null ? sumFilter.getSubtractColumn().resolve() : null;
		List<Column> distinctByColumns = sumFilter.getDistinctByColumn().stream().map(ColumnId::resolve).toList();
		String alias = filterContext.getNameGenerator().selectName(sumFilter);
		ConnectorSqlTables tables = filterContext.getTables();

		CommonAggregationSelect<BigDecimal> sumAggregationSelect;
		ConnectorSqlSelects selects;

		if (!distinctByColumns.isEmpty()) {
			sumAggregationSelect =
					createDistinctSumAggregationSelect(sumColumn, distinctByColumns, alias, filterContext.getIds(), tables);
			selects = ConnectorSqlSelects.builder()
										 .preprocessingSelects(sumAggregationSelect.getRootSelects())
										 .aggregationSelect(sumAggregationSelect.getGroupBy())
										 .build();
		}
		else {
			sumAggregationSelect = createSumAggregationSelect(sumColumn, subtractColumn, alias, tables);
			selects = ConnectorSqlSelects.builder()
										 .preprocessingSelects(sumAggregationSelect.getRootSelects())
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

		Column column = filter.getColumn().resolve();
		String tableName = column.getTable().getName();
		String columnName = column.getName();
		Class<? extends Number> numberClass = NumberMapUtil.getType(column);
		Field<? extends Number> field = DSL.field(DSL.name(tableName, columnName), numberClass);

		ColumnId subtractColumn = filter.getSubtractColumn();
		if (subtractColumn == null) {
			return new SumCondition(field, filterContext.getValue()).condition();
		}

		Column resolvedSubtractionColumn = subtractColumn.resolve();
		String subtractColumnName = resolvedSubtractionColumn.getName();
		String subtractTableName = resolvedSubtractionColumn.getTable().getName();
		Field<? extends Number> subtractField = DSL.field(DSL.name(subtractTableName, subtractColumnName), numberClass);
		return new SumCondition(field.minus(subtractField), filterContext.getValue()).condition();
	}

}
