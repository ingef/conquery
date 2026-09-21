package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountQuartersFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountQuartersSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
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
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;

//TODO(FK): this needs a rework. Current implementation makes a sum of the quarters and doesn't take overlapping events into account.
public class CountQuartersSqlAggregator implements SelectConverter<CountQuartersSelect>, FilterConverter<CountQuartersFilter, Range.LongRange>, SqlAggregator {

	@Override
	public ConnectorSqlSelects connectorSelect(CountQuartersSelect countQuartersSelect, SelectContext<ConnectorSqlTables> selectContext) {

		CommonAggregationSelect<?> countAggregationSelect =
				ResolvedAggregationAdapter.convert(
						new BuiltInAggregations.CountQuarters(EntitySchemaAdapter.from(countQuartersSelect)),
						selectContext.getNameGenerator().legacyOperationName(countQuartersSelect.getName()),
						selectContext.getIds(), selectContext.getTables(), selectContext);

		String finalPredecessor = selectContext.getTables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		ExtractingSqlSelect<?> finalSelect = countAggregationSelect.getGroupBy().qualify(finalPredecessor);

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(countAggregationSelect.getRootSelects())
								  .aggregationSelect(countAggregationSelect.getGroupBy())
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public SqlFilters convertToSqlFilter(CountQuartersFilter countQuartersFilter, FilterContext<Range.LongRange> filterContext) {

		CommonAggregationSelect<?> countAggregationSelect =
				ResolvedAggregationAdapter.convert(
						new BuiltInAggregations.CountQuarters(EntitySchemaAdapter.from(countQuartersFilter)),
						filterContext.getNameGenerator().legacyOperationName(countQuartersFilter.getName()),
						filterContext.getIds(), filterContext.getTables(), filterContext);

		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
														 .preprocessingSelects(countAggregationSelect.getRootSelects())
														 .aggregationSelect(countAggregationSelect.getGroupBy())
														 .build();

		String predecessorTableName = filterContext.getTables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		Field<?> qualifiedCountSelect = countAggregationSelect.getGroupBy().qualify(predecessorTableName).select();
		LegacyInclusiveRangeCondition countCondition = new LegacyInclusiveRangeCondition(qualifiedCountSelect, filterContext.getValue());
		WhereClauses whereClauses = WhereClauses.builder().groupFilter(countCondition).build();

		return new SqlFilters(selects, whereClauses);
	}

	@Override
	public Condition convertForTableExport(CountQuartersFilter filter, FilterContext<Range.LongRange> filterContext) {
		Param<Integer> field = DSL.inline(1); // no grouping, count is always 1 per row
		return new LegacyInclusiveRangeCondition(field, filterContext.getValue()).condition();
	}

}
