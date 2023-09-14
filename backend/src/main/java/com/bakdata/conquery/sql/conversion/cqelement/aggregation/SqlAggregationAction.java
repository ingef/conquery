package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.List;

import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

/**
 * Represents a subset of {@link DateAggregationAction}.
 */
interface SqlAggregationAction {

	DateAggregationTables tableNames();

	List<DateAggregationCte> dateAggregationCtes();

	ColumnDateRange getOverlapValidityDate(DateAggregationDates dateAggregationDates, SqlFunctionProvider functionProvider);

	List<SqlSelect> getIntermediateTableSelects(DateAggregationDates dateAggregationDates, List<SqlSelect> carryThroughSelects);

	List<QueryStep> getNoOverlapSelects(DateAggregationContext dateAggregationContext);

	QueryStep getOverlapStep(DateAggregationContext dateAggregationContext);

}
