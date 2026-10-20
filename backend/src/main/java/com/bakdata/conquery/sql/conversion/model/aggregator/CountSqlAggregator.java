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
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
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

	private static final String ROW_NUMBER_ALIAS = "row_number";

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
									  .additionalPredecessor(countAggregationSelect.getAdditionalPredecessor())
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
		List<ExtractingSqlSelect<?>> preprocessingSelects = new ArrayList<>();
		ExtractingSqlSelect<?> countRootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), countColumn.getName(), Object.class);
		preprocessingSelects.add(countRootSelect);

		List<ExtractingSqlSelect<?>> distinctByRootSelects =
				distinctByColumns.stream()
							 .map(column -> new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), Object.class))
							 .collect(Collectors.toList());
		preprocessingSelects.addAll(distinctByRootSelects);

		QueryStep rowNumberCte = createRowNumberCte(ids, countRootSelect, distinctByRootSelects, alias, tables, nameGenerator);
		Field<?> qualifiedCountSelect = countRootSelect.qualify(rowNumberCte.getCteName()).select();
		FieldWrapper<Integer> countGroupBy = new FieldWrapper<>(DSL.nullif(DSL.count(qualifiedCountSelect), 0).as(alias));
		QueryStep rowNumberFilteredCte = createRowNumberFilteredCte(rowNumberCte, countGroupBy, alias, nameGenerator);

		return CommonAggregationSelect.<Integer>builder()
									  .rootSelects(preprocessingSelects)
									  .additionalPredecessor(rowNumberFilteredCte)
									  .groupBy(countGroupBy)
									  .build();
	}

	private static QueryStep createRowNumberCte(
			SqlIdColumns ids,
			SingleColumnSqlSelect countColumnRootSelect,
			List<ExtractingSqlSelect<?>> distinctByRootSelects,
			String alias,
			SqlTables connectorTables,
			NameGenerator nameGenerator
	) {
		String predecessor = connectorTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT);
		SqlIdColumns qualifiedIds = ids.qualify(predecessor);
		SingleColumnSqlSelect qualifiedCountRootSelect = countColumnRootSelect.qualify(predecessor);
		List<Field<?>> qualifiedDistinctByFields = distinctByRootSelects.stream()
																	.map(sqlSelect -> sqlSelect.qualify(predecessor).select())
																	.collect(Collectors.toList());

		List<Field<?>> partitioningFields = Stream.concat(qualifiedIds.toFields().stream(), qualifiedDistinctByFields.stream())
														 .collect(Collectors.toList());
		FieldWrapper<Integer> rowNumber = new FieldWrapper<>(
				DSL.rowNumber().over(DSL.partitionBy(partitioningFields)).as(ROW_NUMBER_ALIAS),
				partitioningFields.stream().map(Field::getName).toArray(String[]::new)
		);

		Selects rowNumberAssignedSelects = Selects.builder()
													  .ids(qualifiedIds)
													  .sqlSelects(List.of(qualifiedCountRootSelect, rowNumber))
													  .build();
		Condition anyDistinctByValuePresent = qualifiedDistinctByFields.stream()
																		.map(Field::isNotNull)
																		.reduce(Condition::or)
																		.orElseThrow();

		return QueryStep.builder()
						.cteName(nameGenerator.cteStepName(CountDistinctCteStep.ROW_NUMBER_ASSIGNED, alias))
						.selects(rowNumberAssignedSelects)
						.fromTable(QueryStep.toTableLike(predecessor))
						.conditions(List.of(anyDistinctByValuePresent))
						.build();
	}

	private static QueryStep createRowNumberFilteredCte(
			QueryStep rowNumberCte,
			FieldWrapper<Integer> countSelect,
			String alias,
			NameGenerator nameGenerator
	) {
		SqlIdColumns ids = rowNumberCte.getQualifiedSelects().getIds();
		Selects rowNumberFilteredSelects = Selects.builder()
													  .ids(ids)
													  .sqlSelect(countSelect)
													  .build();
		Condition firstOccurrence = DSL.field(DSL.name(rowNumberCte.getCteName(), ROW_NUMBER_ALIAS))
													   .eq(DSL.inline(1));

		return QueryStep.builder()
						.cteName(nameGenerator.cteStepName(CountDistinctCteStep.ROW_NUMBER_FILTERED, alias))
						.selects(rowNumberFilteredSelects)
						.fromTable(QueryStep.toTableLike(rowNumberCte.getCteName()))
						.conditions(List.of(firstOccurrence))
						.predecessor(rowNumberCte)
						.groupBy(ids.toFields())
						.build();
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
	private enum CountDistinctCteStep implements CteStep {

		ROW_NUMBER_ASSIGNED("count_distinct_row_number_assigned", null),
		ROW_NUMBER_FILTERED("count_distinct_row_number_filtered", ROW_NUMBER_ASSIGNED);

		private final String suffix;
		private final CountDistinctCteStep predecessor;
	}


}
