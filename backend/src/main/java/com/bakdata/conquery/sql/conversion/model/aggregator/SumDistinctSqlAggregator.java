package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.SumCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Conversion of a {@link SumSelect} with {@link SumSelect#getDistinctByColumn()}. Sum's the values of a column for each row which is distinct by the
 * distinct-by columns by creating 2 additional CTEs. We can't use our usual {@link ConnectorCteStep#PREPROCESSING} CTE for achieving distinctness, because
 * it's used for the conversion of other selects where distinctness by distinct-by columns is not required and would cause wrong results.
 * <p>
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
@Value
public class SumDistinctSqlAggregator implements SqlAggregator {

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

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	public SumDistinctSqlAggregator(
			Column sumColumn,
			List<Column> distinctByColumns,
			String alias,
			IRange<? extends Number, ?> filterValue,
			Field<Object> primaryColumn,
			SqlTables<ConnectorCteStep> connectorTables,
			NameGenerator nameGenerator
	) {
		// preprocessing
		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumColumn.getType());
		ExtractingSqlSelect<? extends Number> sumColumnRootSelect = new ExtractingSqlSelect<>(connectorTables.getRootTable(), sumColumn.getName(), numberClass);
		List<ExtractingSqlSelect<Object>> distinctByRootSelects = distinctByColumns.stream()
																				   .map(column -> new ExtractingSqlSelect<>(
																						   connectorTables.getRootTable(), column.getName(), Object.class)
																				   )
																				   .toList();

		// additional predecessors
		QueryStep rowNumberCte = createRowNumberCte(primaryColumn, sumColumnRootSelect, distinctByRootSelects, alias, connectorTables, nameGenerator);
		Field<? extends Number> rootSelectQualified = sumColumnRootSelect.qualify(rowNumberCte.getCteName()).select();
		FieldWrapper<BigDecimal> distinctSum = new FieldWrapper<>(DSL.sum(rootSelectQualified).as(alias));
		QueryStep rowNumberFilteredCte = createRowNumberFilteredCte(rowNumberCte, primaryColumn, distinctSum, alias, nameGenerator);

		SqlSelects.SqlSelectsBuilder builder = SqlSelects.builder()
														 .preprocessingSelect(sumColumnRootSelect)
														 .preprocessingSelects(distinctByRootSelects)
														 .additionalPredecessor(Optional.of(rowNumberFilteredCte));

		if (filterValue != null) {
			this.sqlSelects = builder.build();
			String groupFilterPredecessor = connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_FILTER);
			Field<BigDecimal> qualifiedSumSelect = distinctSum.qualify(groupFilterPredecessor).select();
			SumCondition sumCondition = new SumCondition(qualifiedSumSelect, filterValue);
			this.whereClauses = WhereClauses.builder()
											.groupFilter(sumCondition)
											.build();
		}
		else {
			ExtractingSqlSelect<BigDecimal> finalSelect = distinctSum.qualify(connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_FILTER));
			this.sqlSelects = builder.finalSelect(finalSelect).build();
			this.whereClauses = WhereClauses.empty();
		}
	}

	public static SumDistinctSqlAggregator create(SumSelect sumSelect, SelectContext selectContext) {
		return new SumDistinctSqlAggregator(
				sumSelect.getColumn(),
				sumSelect.getDistinctByColumn(),
				selectContext.getNameGenerator().selectName(sumSelect),
				null,
				selectContext.getParentContext().getPrimaryColumn(),
				selectContext.getConnectorTables(),
				selectContext.getNameGenerator()
		);
	}

	public static <RANGE extends IRange<? extends Number, ?>> SumDistinctSqlAggregator create(SumFilter<RANGE> sumFilter, FilterContext<RANGE> filterContext) {
		return new SumDistinctSqlAggregator(
				sumFilter.getColumn(),
				sumFilter.getDistinctByColumn(),
				filterContext.getNameGenerator().selectName(sumFilter),
				filterContext.getValue(),
				filterContext.getParentContext().getPrimaryColumn(),
				filterContext.getConnectorTables(),
				filterContext.getNameGenerator()
		);
	}

	/**
	 * Assigns row numbers for each partition over the pid and the distinct by columns. If the values per pid in the distinct by columns are duplicated,
	 * the row number will be incremented for each duplicated entry.
	 */
	private static QueryStep createRowNumberCte(
			Field<Object> primaryColumn,
			ExtractingSqlSelect<? extends Number> sumColumnRootSelect,
			List<ExtractingSqlSelect<Object>> distinctByRootSelects,
			String alias,
			SqlTables<ConnectorCteStep> conceptTables,
			NameGenerator nameGenerator
	) {
		String predecessor = conceptTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT);

		Field<Object> qualifiedPrimaryColumn = QualifyingUtil.qualify(primaryColumn, predecessor);
		ExtractingSqlSelect<?> qualifiedSumRootSelect = sumColumnRootSelect.qualify(predecessor);

		List<Field<?>> partitioningFields = Stream.concat(
														  Stream.of(qualifiedPrimaryColumn),
														  distinctByRootSelects.stream().map(sqlSelect -> sqlSelect.qualify(predecessor).select())
												  )
												  .collect(Collectors.toList());
		FieldWrapper<Integer> rowNumber = new FieldWrapper<>(
				DSL.rowNumber().over(DSL.partitionBy(partitioningFields)).as(ROW_NUMBER_ALIAS),
				partitioningFields.stream().map(Field::getName).toArray(String[]::new)
		);

		Selects rowNumberAssignedSelects = Selects.builder()
												  .primaryColumn(qualifiedPrimaryColumn)
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
			Field<Object> primaryColumn,
			FieldWrapper<BigDecimal> sumSelect,
			String alias,
			NameGenerator nameGenerator
	) {
		Selects rowNumberFilteredSelects = Selects.builder()
												  .primaryColumn(primaryColumn)
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
						.groupBy(List.of(primaryColumn))
						.build();
	}

}
