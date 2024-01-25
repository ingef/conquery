package com.bakdata.conquery.sql.conversion.model.select;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.SumCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import lombok.Getter;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
public class SumDistinctSqlAggregator implements SqlAggregator {

	@Getter
	private enum SumDistinctCteStep implements CteStep {

		GROUP_BY_DISTINCT_COLUMNS("grouped_by_distinct_columns", null),
		SUM_DISTINCT("sum_distinct", GROUP_BY_DISTINCT_COLUMNS);

		private final String suffix;
		private final SumDistinctCteStep predecessor;

		SumDistinctCteStep(String suffix, SumDistinctCteStep predecessor) {
			this.suffix = suffix;
			this.predecessor = predecessor;
		}

		@Override
		public String cteName(String nodeLabel) {
			return "%s-%s".formatted(nodeLabel, this.suffix);
		}
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
			SqlTables<ConnectorCteStep> conceptTables,
			SqlFunctionProvider functionProvider,
			NameGenerator nameGenerator
	) {
		// preprocesssing
		String rootTable = conceptTables.getRootTable();
		Class<? extends Number> numberClass1 = NumberMapUtil.NUMBER_MAP.get(sumColumn.getType());
		ExtractingSqlSelect<? extends Number> sumColumnRootSelect = new ExtractingSqlSelect<>(rootTable, sumColumn.getName(), numberClass1);
		List<ExtractingSqlSelect<Object>> distinctByRootSelects = distinctByColumns.stream()
																				   .map(column -> new ExtractingSqlSelect<>(
																						   rootTable, column.getName(), Object.class)
																				   )
																				   .toList();

		// sum column grouped by distinct columns
		String predecessor = conceptTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT);
		ExtractingSqlSelect<? extends Number> qualifiedRootSelect = sumColumnRootSelect.createAliasedReference(predecessor);
		FieldWrapper<? extends Number> firstSelect = new FieldWrapper<>(functionProvider.first(qualifiedRootSelect.select(), List.of()).as(alias));
		QueryStep distinctColumnsStep = getGroupByDistinctColumnsStep(alias, primaryColumn, nameGenerator, predecessor, firstSelect, distinctByRootSelects);

		// sum select
		Field<? extends Number> firstSumColumn = firstSelect.createAliasedReference(distinctColumnsStep.getCteName()).select();
		FieldWrapper<BigDecimal> distinctSum = new FieldWrapper<>(DSL.sum(firstSumColumn).as(alias));

		// sum aggregation
		QueryStep sumDistinctCte = getSumDistinctStep(alias, primaryColumn, nameGenerator, distinctSum, distinctColumnsStep);

		SqlSelects.SqlSelectsBuilder builder = SqlSelects.builder()
														 .preprocessingSelect(sumColumnRootSelect)
														 .preprocessingSelects(distinctByRootSelects)
														 .additionalPredecessor(sumDistinctCte);

		if (filterValue != null) {
			this.sqlSelects = builder.build();
			Field<BigDecimal>
					qualifiedSumSelect =
					distinctSum.createAliasedReference(conceptTables.getPredecessor(ConnectorCteStep.AGGREGATION_FILTER)).select();
			SumCondition sumCondition = new SumCondition(qualifiedSumSelect, filterValue);
			this.whereClauses = WhereClauses.builder()
											.groupFilter(sumCondition)
											.build();
		}
		else {
			ExtractingSqlSelect<BigDecimal> finalSelect = distinctSum.createAliasedReference(conceptTables.getPredecessor(ConnectorCteStep.FINAL));
			this.sqlSelects = builder.finalSelect(finalSelect).build();
			this.whereClauses = WhereClauses.builder().build();
		}
	}

	public static SumDistinctSqlAggregator create(SumSelect sumSelect, SelectContext selectContext) {
		return new SumDistinctSqlAggregator(
				sumSelect.getColumn(),
				sumSelect.getDistinctByColumn(),
				selectContext.getNameGenerator().selectName(sumSelect),
				null,
				selectContext.getParentContext().getPrimaryColumn(),
				selectContext.getConceptTables(),
				selectContext.getParentContext().getSqlDialect().getFunctionProvider(),
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
				filterContext.getConceptTables(),
				filterContext.getParentContext().getSqlDialect().getFunctionProvider(),
				filterContext.getNameGenerator()
		);
	}

	private static QueryStep getSumDistinctStep(
			String alias,
			Field<Object> primaryColumn,
			NameGenerator nameGenerator,
			FieldWrapper<BigDecimal> distinctSum,
			QueryStep distinctColumnsStep
	) {
		Field<Object> qualifiedPrimaryColumn = QualifyingUtil.qualify(primaryColumn, distinctColumnsStep.getCteName());

		Selects sumDistinctSelects = Selects.builder()
											.primaryColumn(qualifiedPrimaryColumn)
											.sqlSelect(distinctSum)
											.build();

		return QueryStep.builder()
						.cteName(nameGenerator.cteStepName(SumDistinctCteStep.SUM_DISTINCT, alias))
						.selects(sumDistinctSelects)
						.fromTable(QueryStep.toTableLike(distinctColumnsStep.getCteName()))
						.predecessors(List.of(distinctColumnsStep))
						.groupBy(List.of(qualifiedPrimaryColumn))
						.build();
	}

	private static QueryStep getGroupByDistinctColumnsStep(
			String alias,
			Field<Object> primaryColumn,
			NameGenerator nameGenerator,
			String predecessor,
			FieldWrapper<? extends Number> firstSelect,
			List<ExtractingSqlSelect<Object>> distinctByRootSelects
	) {
		Field<Object> qualifiedPrimaryColumn = QualifyingUtil.qualify(primaryColumn, predecessor);
		Selects selects = Selects.builder()
								 .primaryColumn(qualifiedPrimaryColumn)
								 .sqlSelect(firstSelect)
								 .build();

		List<Field<?>> groupByFields = Stream.concat(
				Stream.of(qualifiedPrimaryColumn),
				distinctByRootSelects.stream().map(sqlSelect -> sqlSelect.createAliasedReference(predecessor)).map(ExtractingSqlSelect::select)
		).collect(Collectors.toList());

		return QueryStep.builder()
						.cteName(nameGenerator.cteStepName(SumDistinctCteStep.GROUP_BY_DISTINCT_COLUMNS, alias))
						.selects(selects)
						.fromTable(QueryStep.toTableLike(predecessor))
						.groupBy(groupByFields)
						.build();
	}

}
