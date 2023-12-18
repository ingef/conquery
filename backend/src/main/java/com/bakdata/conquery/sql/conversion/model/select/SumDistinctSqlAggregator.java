package com.bakdata.conquery.sql.conversion.model.select;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.NumberMapUtil;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.filter.SumCondition;
import lombok.Value;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
public class SumDistinctSqlAggregator implements SqlAggregator {

	private enum SumDistinctCteStep implements CteStep {

		ROW_NUMBER_ASSIGNED("row_number_assigned", null),
		ROW_NUMBER_FILTERED("row_number_filtered", ROW_NUMBER_ASSIGNED);

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

		@Override
		public CteStep predecessor() {
			return this.predecessor;
		}
	}

	private static final String ROW_NUMBER_ALIAS = "row_number";
	private static final String SUM_DISTINCT_SUFFIX = "sum_distinct";

	SqlSelects sqlSelects;
	Filters filters;

	public SumDistinctSqlAggregator(
			Column sumColumn,
			List<Column> distinctByColumns,
			String alias,
			IRange<? extends Number, ?> filterValue,
			Field<Object> primaryColumn,
			SqlTables<ConceptCteStep> conceptTables,
			NameGenerator nameGenerator
	) {
		String rootTable = conceptTables.getRootTable();
		Class<? extends Number> numberClass1 = NumberMapUtil.NUMBER_MAP.get(sumColumn.getType());
		ExtractingSqlSelect<? extends Number> sumColumnRootSelect = new ExtractingSqlSelect<>(rootTable, sumColumn.getName(), numberClass1);
		List<ExtractingSqlSelect<Object>> distinctByRootSelects = distinctByColumns.stream()
																				   .map(column -> new ExtractingSqlSelect<>(
																						   rootTable, column.getName(), Object.class)
																				   )
																				   .toList();

		QueryStep rowNumberCte = createRowNumberCte(primaryColumn, sumColumnRootSelect, distinctByRootSelects, alias, conceptTables, nameGenerator);
		FieldWrapper<BigDecimal> sumSelect = createSumDistinctSelect(sumColumnRootSelect, alias, rowNumberCte);
		QueryStep rowNumberFilteredCte = createRowNumberFilteredCte(rowNumberCte, primaryColumn, sumSelect, alias, nameGenerator);

		SqlSelects.SqlSelectsBuilder builder = SqlSelects.builder()
														 .preprocessingSelect(sumColumnRootSelect)
														 .preprocessingSelects(distinctByRootSelects)
														 .additionalPredecessor(rowNumberFilteredCte);

		if (filterValue != null) {
			this.sqlSelects = builder.build();
			Field<BigDecimal> qualifiedSumSelect = sumSelect.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER)).select();
			SumCondition sumCondition = new SumCondition(qualifiedSumSelect, filterValue);
			this.filters = Filters.builder()
								  .group(List.of(sumCondition))
								  .build();
		}
		else {
			ExtractingSqlSelect<BigDecimal> finalSelect = sumSelect.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.FINAL));
			this.sqlSelects = builder.finalSelect(finalSelect).build();
			this.filters = Filters.builder().build();
		}
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
			SqlTables<ConceptCteStep> conceptTables,
			NameGenerator nameGenerator
	) {
		String predecessor = conceptTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT);

		Field<Object> qualifiedPrimaryColumn = QualifyingUtil.qualify(primaryColumn, predecessor);
		ExtractingSqlSelect<?> qualifiedSumRootSelect = sumColumnRootSelect.createColumnReference(predecessor);

		List<Field<?>> partitioningFields = Stream.concat(
				Stream.of(qualifiedPrimaryColumn),
				distinctByRootSelects.stream().map(sqlSelect -> sqlSelect.createColumnReference(predecessor).select())
		).toList();
		FieldWrapper<Integer> rowNumber = new FieldWrapper<>(
				DSL.rowNumber()
				   .over(DSL.partitionBy(partitioningFields))
				   .as(ROW_NUMBER_ALIAS)
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

	public static SumDistinctSqlAggregator create(SumSelect sumSelect, SelectContext selectContext) {
		return new SumDistinctSqlAggregator(
				sumSelect.getColumn(),
				sumSelect.getDistinctByColumn(),
				selectContext.getNameGenerator().selectName(sumSelect),
				null,
				selectContext.getParentContext().getPrimaryColumn(),
				selectContext.getConceptTables(),
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
				filterContext.getNameGenerator()
		);
	}

	private static FieldWrapper<BigDecimal> createSumDistinctSelect(ExtractingSqlSelect<? extends Number> sumColumnRootSelect, String alias, QueryStep rowNumberCte) {
		Field<? extends Number> rootSelectQualified = sumColumnRootSelect.createAliasedReference(rowNumberCte.getCteName()).select();
		return new FieldWrapper<>(DSL.sum(rootSelectQualified).as(alias));
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
