package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.List;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import lombok.Data;
import lombok.ToString;

/**
 * Aggregator, counting the number of days present.
 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class TwoColumnDurationSumAggregator extends ColumnAggregator<Long> {

	@ToString.Include
	private final Column startColumn, endColumn;

	private CDateSet set = CDateSet.createEmpty();
	private CDateSet dateRestriction;

	private int realUpperBound;


	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		set.clear();
		realUpperBound = context.getToday();
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public List<Column> getRequiredColumns() {
		return List.of(startColumn, endColumn);
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
		if (!bucket.has(event, startColumn)) {
			return;
		}

		if (!bucket.has(event, endColumn)) {
			return;
		}

		final int begin = bucket.getDate(event, startColumn);
		final int end = bucket.getDate(event, endColumn);

		set.maskedAdd(CDateRange.of(begin, end), dateRestriction, realUpperBound);
	}

	@Override
	public Long createAggregationResult() {
		if (set.isEmpty() || CDate.isNegativeInfinity(set.getMinValue())) {
			return null;
		}
		return set.countDays();
	}

}
