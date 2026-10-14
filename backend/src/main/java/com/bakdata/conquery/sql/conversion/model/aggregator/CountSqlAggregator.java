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
import lombok.NoArgsConstructor;
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
		String alias = selectContext.getNameGenerator().selectName(countSelect);
		List<Column> distinctByColumns = countSelect.getDistinctByColumn() == null
										 ? List.of()
										 : countSelect.getDistinctByColumn().stream().map(ColumnId::resolve).toList();

		CommonAggregationSelect<Integer> countAggregationSelect = distinct && !distinctByColumns.isEmpty()
															 ? createDistinctCountAggregationSelect(countColumn, distinctByColumns, alias, selectContext.getIds(), tables)
															 : createCountAggregationSelect(countColumn, distinct, alias, tables);

		String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		ExtractingSqlSelect<Integer> finalSelect = countAggregationSelect.getGroupBy().qualify(finalPredecessor);

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(countAggregationSelect.getRootSelects())
								  .aggregationSelect(countAggregationSelect.getGroupBy())
								  .finalSelect(finalSelect)
								  .build();
	}

	private static CommonAggregationSelect<Integer> createDistinctCountAggregationSelect(
			Column countColumn,
			List<Column> distinctByColumns,
			String alias,
			SqlIdColumns ids,
			ConnectorSqlTables tables
	) {
		List<SingleColumnSqlSelect> preprocessingSelects = new ArrayList<>();
		ExtractingSqlSelect<?> countRootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), countColumn.getName(), Object.class);
		preprocessingSelects.add(countRootSelect);

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
		Field<?> qualifiedCountSelect = countRootSelect.qualify(preprocessingCte).select();
		Field<Integer> qualifiedRowNumber = rowNumber.qualify(preprocessingCte).select();
		Condition anyDistinctByValuePresent = distinctByRootSelects.stream()
																		  .map(select -> select.qualify(preprocessingCte).select().isNotNull())
																		  .reduce(Condition::or)
																		  .orElseThrow();
		Condition countDistinctValue = qualifiedRowNumber.eq(DSL.inline(1)).and(anyDistinctByValuePresent);
		Field<?> distinctCountValue = DSL.when(countDistinctValue, qualifiedCountSelect);
		FieldWrapper<Integer> countGroupBy = new FieldWrapper<>(DSL.nullif(DSL.count(distinctCountValue), 0).as(alias));

		return CommonAggregationSelect.<Integer>builder()
									  .rootSelects(preprocessingSelects)
									  .groupBy(countGroupBy)
									  .build();
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



}
