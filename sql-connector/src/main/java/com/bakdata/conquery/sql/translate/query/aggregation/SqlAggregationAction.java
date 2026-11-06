package com.bakdata.conquery.sql.translate.query.aggregation;

import java.util.List;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;

/**
 * Represents a subset of {@link DateAggregationAction}.
 */
interface SqlAggregationAction {

	SqlTables tableNames(SqlNameGenerator nameGenerator);

	List<DateAggregationCte> dateAggregationCtes();

	ColumnDateRange getOverlapValidityDate(DateAggregationDates dateAggregationDates);

	List<SqlSelect> getIntermediateTableSelects(DateAggregationDates dateAggregationDates, List<SqlSelect> carryThroughSelects);

	List<QueryStep> getNoOverlapSelects(DateAggregationContext dateAggregationContext);

	QueryStep getOverlapStep(DateAggregationContext dateAggregationContext);

	boolean requiresIntervalPackingAfterwards();

}
