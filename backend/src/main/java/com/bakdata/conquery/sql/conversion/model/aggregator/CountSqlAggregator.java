package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.util.List;

import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.LegacyInclusiveRangeCondition;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import lombok.NoArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;

@NoArgsConstructor
public class CountSqlAggregator implements SelectConverter<CountSelect>, FilterConverter<CountFilter, Range.LongRange>, SqlAggregator {

	@Override
	public ConnectorSqlSelects connectorSelect(CountSelect countSelect, SelectContext<ConnectorSqlTables> selectContext) {

		SqlTables tables = selectContext.getTables();
		boolean distinct = countSelect.isDistinct();
		Column countColumn = countSelect.getColumn().resolve();
		String alias = selectContext.getNameGenerator().legacyOperationName(countSelect.getName());

		CommonAggregationSelect<?> countAggregationSelect = createCountAggregationSelect(countColumn, distinct, alias, tables, selectContext.getIds(), selectContext);

		String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		ExtractingSqlSelect<?> finalSelect = countAggregationSelect.getGroupBy().qualify(finalPredecessor);

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(countAggregationSelect.getRootSelects())
								  .aggregationSelect(countAggregationSelect.getGroupBy())
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public SqlFilters convertToSqlFilter(CountFilter countFilter, FilterContext<Range.LongRange> filterContext) {

		SqlTables tables = filterContext.getTables();
		boolean distinct = countFilter.isDistinct();
		Column countColumn = countFilter.getColumn().resolve();
		String alias = filterContext.getNameGenerator().legacyOperationName(countFilter.getName());

		CommonAggregationSelect<?> countAggregationSelect = createCountAggregationSelect(countColumn, distinct, alias, tables, filterContext.getIds(), filterContext);
		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
														 .preprocessingSelects(countAggregationSelect.getRootSelects())
														 .aggregationSelect(countAggregationSelect.getGroupBy())
														 .build();

		Field<?> qualifiedCountSelect = countAggregationSelect.getGroupBy().qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER)).select();
		LegacyInclusiveRangeCondition countCondition = new LegacyInclusiveRangeCondition(qualifiedCountSelect, filterContext.getValue());
		WhereClauses whereClauses = WhereClauses.builder()
												.groupFilter(countCondition)
												.build();

		return new SqlFilters(selects, whereClauses);
	}

	@Override
	public Condition convertForTableExport(CountFilter countFilter, FilterContext<Range.LongRange> filterContext) {
		Param<Integer> field = DSL.inline(1); // no grouping, count is always 1 per row
		return new LegacyInclusiveRangeCondition(field, filterContext.getValue()).condition();
	}
	private static CommonAggregationSelect<?> createCountAggregationSelect(
			Column countColumn, boolean distinct, String alias, SqlTables tables,
			SqlIdColumns ids, Context context
	) {
		ResolvedColumn column = EntitySchemaAdapter.from(countColumn);
		// Existing SQL COUNT DISTINCT uses the counted column, not distinctByColumn.
		return ResolvedAggregationAdapter.convert(new BuiltInAggregations.Count(column, distinct ? List.of(column) : List.of()),
				alias, ids, tables, context);
	}
}
