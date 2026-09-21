package com.bakdata.conquery.sql.conversion.model.aggregator;

import static org.jooq.impl.DSL.field;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.LongRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.DurationSumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DurationSumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.LegacyInclusiveRangeCondition;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class DurationSumSqlAggregator implements SelectConverter<DurationSumSelect>, FilterConverter<DurationSumFilter, Range.LongRange>, SqlAggregator {

	@Override
	public ConnectorSqlSelects connectorSelect(DurationSumSelect select, SelectContext<ConnectorSqlTables> selectContext) {
		String alias = selectContext.getNameGenerator().legacyOperationName(select.getName());
		var aggregation = ResolvedAggregationAdapter.convert(
				new BuiltInAggregations.DurationSum(EntitySchemaAdapter.from(select), List.of()),
				alias, selectContext.getIds(), selectContext.getTables(), selectContext);
		return ConnectorSqlSelects.builder()
				.preprocessingSelects(aggregation.getRootSelects())
				.additionalPredecessor(aggregation.getAdditionalPredecessor())
				.finalSelect(aggregation.getGroupBy().qualify(selectContext.getTables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER)))
				.build();
	}

	@Override
	public SqlFilters convertToSqlFilter(DurationSumFilter filter, FilterContext<LongRange> context) {
		String alias = context.getNameGenerator().legacyOperationName(filter.getName());
		var aggregation = ResolvedAggregationAdapter.convert(
				new BuiltInAggregations.DurationSum(EntitySchemaAdapter.from(filter), List.of()),
				alias, context.getIds(), context.getTables(), context);
		Field<?> field = aggregation.getGroupBy().qualify(context.getTables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER)).select();
		return new SqlFilters(ConnectorSqlSelects.builder()
				.preprocessingSelects(aggregation.getRootSelects())
				.additionalPredecessor(aggregation.getAdditionalPredecessor()).build(),
				WhereClauses.builder().groupFilter(new LegacyInclusiveRangeCondition(field, context.getValue())).build());
	}

	@Override
	public Condition convertForTableExport(DurationSumFilter filter, FilterContext<LongRange> filterContext) {
		SqlFunctionProvider functionProvider = filterContext.getFunctionProvider();

		Field<Date> startDateField;
		Field<Date> endDateField;

		if (filter.isSingleColumnDaterange()) {
			Column column = filter.getColumn().resolve();
			String tableName = column.getTable().getName();
			Field<Date> daterangeField = field(DSL.name(tableName, column.getName()), Date.class);
			startDateField = daterangeField;
			endDateField = daterangeField;
		}
		else {
			Column startColumn = filter.getStartColumn().resolve();
			Column endColumn = filter.getEndColumn().resolve();
			String tableName = startColumn.getTable().getName();
			startDateField = field(DSL.name(tableName, startColumn.getName()), Date.class);
			endDateField = field(DSL.name(tableName, endColumn.getName()), Date.class);
		}
		Field<Integer> dateDistance = functionProvider.dateDistance(ChronoUnit.DAYS, startDateField, endDateField);
		// no need so compute a sum here - the duration sum of a single row is simply the date distance
		return new LegacyInclusiveRangeCondition(dateDistance, filterContext.getValue()).condition();
	}

}
