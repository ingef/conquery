package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.conversion.model.NumberMapUtil;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.LegacyInclusiveRangeCondition;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
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
 *   			  from "preprocessing"
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

	@Override
	public ConnectorSqlSelects connectorSelect(SumSelect sumSelect, SelectContext<ConnectorSqlTables> selectContext) {

		SqlNameGenerator nameGenerator = selectContext.getNameGenerator();
		String alias = nameGenerator.legacyOperationName(sumSelect.getName());

		Column sumColumn = sumSelect.getColumn().resolve();
		Column subtractColumn = sumSelect.getSubtractColumn() != null ? sumSelect.getSubtractColumn().resolve() : null;

		List<Column> distinctByColumns = sumSelect.getDistinctByColumn().stream().map(ColumnId::resolve).toList();

		SqlTables tables = selectContext.getTables();

		CommonAggregationSelect<?> sumAggregationSelect;

		if (!distinctByColumns.isEmpty()) {
			SqlIdColumns ids = selectContext.getIds();
			sumAggregationSelect = createSumAggregationSelect(sumColumn, subtractColumn, distinctByColumns, alias, ids, tables, selectContext);
			ExtractingSqlSelect<?> finalSelect = createFinalSelect(sumAggregationSelect, tables);
			return ConnectorSqlSelects.builder()
									  .preprocessingSelects(sumAggregationSelect.getRootSelects())
									  .additionalPredecessor(sumAggregationSelect.getAdditionalPredecessor())
									  .finalSelect(finalSelect)
									  .build();
		}
		else {
			sumAggregationSelect = createSumAggregationSelect(sumColumn, subtractColumn, distinctByColumns, alias, selectContext.getIds(), tables, selectContext);
			ExtractingSqlSelect<?> finalSelect = createFinalSelect(sumAggregationSelect, tables);
			return ConnectorSqlSelects.builder()
									  .preprocessingSelects(sumAggregationSelect.getRootSelects())
									  .aggregationSelect(sumAggregationSelect.getGroupBy())
									  .finalSelect(finalSelect)
									  .build();
		}
	}

	@Override
	public SqlFilters convertToSqlFilter(SumFilter<RANGE> sumFilter, FilterContext<RANGE> filterContext) {

		Column sumColumn = sumFilter.getColumn().resolve();
		Column subtractColumn = sumFilter.getSubtractColumn() != null ? sumFilter.getSubtractColumn().resolve() : null;
		List<Column> distinctByColumns = sumFilter.getDistinctByColumn().stream().map(ColumnId::resolve).toList();
		String alias = filterContext.getNameGenerator().legacyOperationName(sumFilter.getName());
		SqlTables tables = filterContext.getTables();

		CommonAggregationSelect<?> sumAggregationSelect;
		ConnectorSqlSelects selects;

		if (!distinctByColumns.isEmpty()) {
			sumAggregationSelect =
					createSumAggregationSelect(sumColumn, subtractColumn, distinctByColumns, alias, filterContext.getIds(), tables, filterContext);
			selects = ConnectorSqlSelects.builder()
										 .preprocessingSelects(sumAggregationSelect.getRootSelects())
										 .additionalPredecessor(sumAggregationSelect.getAdditionalPredecessor())
										 .build();
		}
		else {
			sumAggregationSelect = createSumAggregationSelect(sumColumn, subtractColumn, distinctByColumns, alias, filterContext.getIds(), tables, filterContext);
			selects = ConnectorSqlSelects.builder()
										 .preprocessingSelects(sumAggregationSelect.getRootSelects())
										 .additionalPredecessor(sumAggregationSelect.getAdditionalPredecessor())
										 .aggregationSelect(sumAggregationSelect.getGroupBy())
										 .build();
		}

		Field<?> qualifiedSumSelect = sumAggregationSelect.getGroupBy().qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER)).select();
		LegacyInclusiveRangeCondition sumCondition = new LegacyInclusiveRangeCondition(qualifiedSumSelect, filterContext.getValue());
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
			return new LegacyInclusiveRangeCondition(field, filterContext.getValue()).condition();
		}

		Column resolvedSubtractionColumn = subtractColumn.resolve();
		String subtractColumnName = resolvedSubtractionColumn.getName();
		String subtractTableName = resolvedSubtractionColumn.getTable().getName();
		Field<? extends Number> subtractField = DSL.field(DSL.name(subtractTableName, subtractColumnName), numberClass);
		return new LegacyInclusiveRangeCondition(field.minus(subtractField), filterContext.getValue()).condition();
	}

	private static CommonAggregationSelect<?> createSumAggregationSelect(
			Column sumColumn, Column subtractColumn, List<Column> distinctByColumns,
			String alias, SqlIdColumns ids, SqlTables tables, Context context
	) {
		return ResolvedAggregationAdapter.convert(new BuiltInAggregations.Sum(
				EntitySchemaAdapter.from(sumColumn),
				Optional.ofNullable(subtractColumn).map(EntitySchemaAdapter::from),
				distinctByColumns.stream().map(EntitySchemaAdapter::from).toList()
		), alias, ids, tables, context);
	}

	private static ExtractingSqlSelect<?> createFinalSelect(CommonAggregationSelect<?> aggregation, SqlTables tables) {
		return aggregation.getGroupBy().qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));
	}
}
