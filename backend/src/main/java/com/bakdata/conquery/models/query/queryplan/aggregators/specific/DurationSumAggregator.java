package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.DateAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import java.util.Optional;

/**
 * Aggregator, counting the number of days present.
 */
public class DurationSumAggregator extends SingleColumnAggregator<Long> {

	private Optional<Aggregator<CDateSet>> queryDateAggregator;
	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;
	private Column validityDateColumn;

	public DurationSumAggregator(Column column) {
		super(column);
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
		validityDateColumn = ctx.getValidityDateColumn();
		queryDateAggregator = ctx.getQueryDateAggregator();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		final CDateRange value = bucket.getAsDateRange(event, getColumn());

		if (value.isOpen()) {
			return;
		}


		set.maskedAdd(value, dateRestriction);
	}

	@Override
	public DurationSumAggregator doClone(CloneContext ctx) {
		return new DurationSumAggregator(getColumn());
	}

	@Override
	public Long getAggregationResult() {
		if (getColumn().equals(validityDateColumn) && queryDateAggregator.isPresent()) {
			set.retainAll(queryDateAggregator.get().getAggregationResult());
		}
		return set.countDays();
	}

	@Override
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}
