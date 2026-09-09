package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.List;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.ir.DateAggregationDates;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.aggregation.DateAggregationCompiler;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;

/**
 * Adapts the legacy backend date-aggregation contract to the connector compiler.
 *
 * <p>TODO Remove this adapter when backend converters invoke {@link DateAggregationCompiler} directly.</p>
 */
public class AnsiSqlDateAggregator implements SqlDateAggregator {

	@Override
	public QueryStep apply(
			QueryStep joinedStep,
			List<SqlSelect> carryThroughSelects,
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction,
			ConversionContext conversionContext
	) {
		return DateAggregationCompiler.aggregate(
				joinedStep,
				carryThroughSelects,
				dateAggregationDates,
				dateAggregationAction,
				conversionContext.getCompilerDialect(),
				conversionContext.getNameGenerator()
		);
	}

	@Override
	public ColumnDateRange getAggregatedValidityDate(
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction
	) {
		return DateAggregationCompiler.getAggregatedValidityDate(dateAggregationDates);
	}

	@Override
	public QueryStep invertAggregatedIntervals(QueryStep baseStep, ConversionContext conversionContext) {
		return DateAggregationCompiler.invert(
				baseStep,
				conversionContext.getCompilerDialect(),
				conversionContext.getNameGenerator()
		);
	}
}
