package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.filter.CountCondition;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SingleColumnSqlSelect;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;

@NoArgsConstructor
public class CountSqlAggregator implements SelectConverter<CountSelect>, FilterConverter<CountFilter, Range.LongRange>, SqlAggregator {

	@Override
	public ConnectorSqlSelects connectorSelect(CountSelect countSelect, SelectContext<ConnectorSqlTables> selectContext) {

		ConnectorSqlTables tables = selectContext.getTables();
		boolean distinct = countSelect.isDistinct();
		Column countColumn = countSelect.getColumn().resolve();
		NameGenerator nameGenerator = selectContext.getNameGenerator();
		String alias = nameGenerator.selectName(countSelect);
		List<Column> distinctByColumns = countSelect.getDistinctByColumn() == null
										 ? List.of()
										 : countSelect.getDistinctByColumn().stream().map(ColumnId::resolve).toList();

		CommonAggregationSelect<Integer> countAggregationSelect;
		if (distinct && !distinctByColumns.isEmpty()) {
			countAggregationSelect = createDistinctCountAggregationSelect(
					countColumn,
					distinctByColumns,
					alias,
					selectContext.getIds(),
					tables,
					nameGenerator
			);

			return ConnectorSqlSelects.builder()
									  .preprocessingSelects(countAggregationSelect.getRootSelects())
									  .aggregationSelect(countAggregationSelect.getGroupBy())
									  .finalSelect(createFinalSelect(countAggregationSelect, tables))
									  .build();
		}

		countAggregationSelect = createCountAggregationSelect(countColumn, distinct, alias, tables);

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(countAggregationSelect.getRootSelects())
								  .aggregationSelect(countAggregationSelect.getGroupBy())
								  .finalSelect(createFinalSelect(countAggregationSelect, tables))
								  .build();
	}

	private static CommonAggregationSelect<Integer> createDistinctCountAggregationSelect(
			Column countColumn,
			List<Column> distinctByColumns,
			String alias,
			SqlIdColumns ids,
			ConnectorSqlTables tables,
			NameGenerator nameGenerator
	) {
		final List<SingleColumnSqlSelect> preprocessingSelects = new ArrayList<>();
		final ExtractingSqlSelect<?> countRootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), countColumn.getName(), Object.class);
		preprocessingSelects.add(countRootSelect);

		final List<ExtractingSqlSelect<?>> distinctByRootSelects =
				distinctByColumns.stream()
							 .map(column -> new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), Object.class))
							 .collect(Collectors.toList());
		preprocessingSelects.addAll(distinctByRootSelects);

		final String rowNumberAlias = nameGenerator.cteStepName(CountDistinctStep.ROW_NUMBER, alias);
		final FieldWrapper<Integer> rowNumberSelect = createRowNumberSelect(ids, distinctByRootSelects, rowNumberAlias);
		preprocessingSelects.add(rowNumberSelect);

		final String aggregationPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT);
		final Field<?> qualifiedCountSelect = countRootSelect.qualify(aggregationPredecessor).select();
		final List<Field<?>> qualifiedDistinctByFields = distinctByRootSelects.stream()
																			.map(sqlSelect -> sqlSelect.qualify(aggregationPredecessor).select())
																			.collect(Collectors.toList());
		final Field<Integer> qualifiedRowNumber = rowNumberSelect.qualify(aggregationPredecessor).select();

		final Condition firstOccurrence = qualifiedRowNumber.eq(DSL.inline(1));
		final Condition anyDistinctByValuePresent = qualifiedDistinctByFields.stream()
																			.map(Field::isNotNull)
																			.reduce(Condition::or)
																			.orElseThrow();
		final Field<?> countValue = DSL.when(firstOccurrence.and(anyDistinctByValuePresent), qualifiedCountSelect);
		final FieldWrapper<Integer> countGroupBy = new FieldWrapper<>(DSL.nullif(DSL.count(countValue), 0).as(alias));

		return CommonAggregationSelect.<Integer>builder()
									  .rootSelects(preprocessingSelects)
									  .groupBy(countGroupBy)
									  .build();
	}

	/**
	 * Assigns row numbers within each partition of subject IDs and distinct-by values during preprocessing. Rows with the same subject and distinct-by tuple
	 * receive increasing row numbers and can therefore be reduced to a single representative during aggregation.
	 */
	private static FieldWrapper<Integer> createRowNumberSelect(
			SqlIdColumns ids,
			List<ExtractingSqlSelect<?>> distinctByRootSelects,
			String rowNumberAlias
	) {
		final SqlIdColumns rootIds = ids.getPredecessor().orElse(ids);
		final List<Field<?>> partitioningFields = Stream.concat(
														rootIds.toFields().stream(),
														distinctByRootSelects.stream().map(SingleColumnSqlSelect::select)
												)
												.collect(Collectors.toList());

		return new FieldWrapper<>(
				DSL.rowNumber().over(DSL.partitionBy(partitioningFields)).as(rowNumberAlias),
				partitioningFields.stream().map(Field::getName).toArray(String[]::new)
		);
	}

	private static ExtractingSqlSelect<Integer> createFinalSelect(
			CommonAggregationSelect<Integer> countAggregationSelect,
			ConnectorSqlTables tables
	) {
		String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		return countAggregationSelect.getGroupBy().qualify(finalPredecessor);
	}

	private CommonAggregationSelect<Integer> createCountAggregationSelect(Column countColumn, boolean distinct, String alias, ConnectorSqlTables tables) {

		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), countColumn.getName(), Object.class);


		Field<?> qualifiedRootSelect = rootSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		Field<Integer> countField = distinct
									? DSL.countDistinct(qualifiedRootSelect)
									: DSL.count(qualifiedRootSelect);
		FieldWrapper<Integer> countGroupBy = new FieldWrapper<>(DSL.nullif(countField, 0).as(alias), countColumn.getName());

		return CommonAggregationSelect.<Integer>builder()
									  .rootSelect(rootSelect)
									  .groupBy(countGroupBy)
									  .build();
	}

	@Override
	public SqlFilters convertToSqlFilter(CountFilter countFilter, FilterContext<Range.LongRange> filterContext) {

		ConnectorSqlTables tables = filterContext.getTables();
		boolean distinct = countFilter.isDistinct();
		Column countColumn = countFilter.getColumn().resolve();
		String alias = filterContext.getNameGenerator().selectName(countFilter);

		CommonAggregationSelect<Integer> countAggregationSelect = createCountAggregationSelect(countColumn, distinct, alias, tables);
		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
														 .preprocessingSelects(countAggregationSelect.getRootSelects())
														 .aggregationSelect(countAggregationSelect.getGroupBy())
														 .build();

		Field<Integer> qualifiedCountSelect = countAggregationSelect.getGroupBy().qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER)).select();
		CountCondition countCondition = new CountCondition(qualifiedCountSelect, filterContext.getValue());
		WhereClauses whereClauses = WhereClauses.builder()
												.groupFilter(countCondition)
												.build();

		return new SqlFilters(selects, whereClauses);
	}

	@Override
	public Condition convertForTableExport(CountFilter countFilter, FilterContext<Range.LongRange> filterContext) {
		Param<Integer> field = DSL.inline(1); // no grouping, count is always 1 per row
		return new CountCondition(field, filterContext.getValue()).condition();
	}

	@Getter
	@RequiredArgsConstructor
	private enum CountDistinctStep implements CteStep {

		ROW_NUMBER("count_distinct_row_number");

		private final String suffix;
	}


}
