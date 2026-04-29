package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.LongRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.DurationSumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DurationSumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.filter.DateDistanceCondition;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.SumCondition;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.DaterangeSelectUtil;
import com.bakdata.conquery.sql.conversion.model.select.DaterangeSelectUtil.AggregationFunction;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import java.sql.Date;
import java.time.temporal.ChronoUnit;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class DurationSumSqlAggregator implements SelectConverter<DurationSumSelect>, FilterConverter<DurationSumFilter, Range.LongRange>, SqlAggregator {

	@Override
	public ConnectorSqlSelects connectorSelect(DurationSumSelect select, SelectContext<ConnectorSqlTables> selectContext) {
		return DaterangeSelectUtil.createForSelect(select, durationSumSelectFunction(), selectContext);
	}

	@Override
	public SqlFilters convertToSqlFilter(DurationSumFilter filter, FilterContext<LongRange> context) {
		return DaterangeSelectUtil.createForFilter(
				filter,
				durationSumSelectFunction(),
				(aggregationField -> new SumCondition((Field<? extends Number>) aggregationField, context.getValue())),
				context);
	}

	@Override
	public Condition convertForTableExport(DurationSumFilter filter, FilterContext<LongRange> filterContext) {
		SqlFunctionProvider functionProvider = filterContext.getFunctionProvider();

		Field<Date> startDateField;
		Field<Date> endDateField;
		if (!filter.isSingleColumnDaterange()) {
			Column startColumn = filter.getStartColumn().resolve();
			Column endColumn = filter.getEndColumn().resolve();
			String tableName = startColumn.getTable().getName();
			startDateField = DSL.field(DSL.name(tableName, startColumn.getName()), Date.class);
			endDateField = DSL.field(DSL.name(tableName, endColumn.getName()), Date.class);
		} else {
			Column column = filter.getColumn().resolve();
			String tableName = column.getTable().getName();
			Field<Date> daterangeField = DSL.field(DSL.name(tableName, column.getName()), Date.class);
			startDateField = functionProvider.lower(daterangeField);
			endDateField = functionProvider.upper(daterangeField);
		}
		Field<Integer> dateDistance = functionProvider.dateDistance(ChronoUnit.DAYS, startDateField, endDateField);
		// no need so compute a sum here - the duration sum of a single row is simply the date distance
		return new DateDistanceCondition(dateDistance, filterContext.getValue()).condition();
	}

	private static AggregationFunction durationSumSelectFunction() {
		return (daterange, alias, functionProvider) -> {
			ColumnDateRange asDualColumn = functionProvider.toDualColumn(daterange);
			return DaterangeSelectUtil.createDurationSumSqlSelect(alias, asDualColumn, functionProvider);
		};
	}
}
