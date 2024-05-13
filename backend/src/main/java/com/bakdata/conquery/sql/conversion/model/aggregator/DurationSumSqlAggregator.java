package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DurationSumSelect;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Value;

@Value
public class DurationSumSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	public static DurationSumSqlAggregator create(DurationSumSelect select, SelectContext context) {

		SqlSelects sqlSelects = DaterangeSelectUtil.createSqlSelects(
				select,
				(daterange, alias, functionProvider) -> {
					ColumnDateRange asDualColumn = functionProvider.toDualColumn(daterange);
					return DaterangeSelectUtil.createDurationSumSqlSelect(alias, asDualColumn, functionProvider);
				},
				context
		);

		return new DurationSumSqlAggregator(
				sqlSelects,
				WhereClauses.builder().build()
		);
	}

}
