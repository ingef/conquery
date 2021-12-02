package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Optional;

import javax.annotation.CheckForNull;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.ToString;

/**
 * Aggregator, counting the number of days present.
 */
@ToString(onlyExplicitlyIncluded = true)
public class EventDurationSumAggregator extends Aggregator<Long> {

	private Optional<Aggregator<CDateSet>> queryDateAggregator = Optional.empty();
	private final CDateSet set = CDateSet.create();

	@CheckForNull
	private CDateSet dateRestriction;
	@CheckForNull
	private Column validityDateColumn;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		set.clear();
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
		validityDateColumn = ctx.getValidityDateColumn();
		queryDateAggregator = ctx.getQueryDateAggregator();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (validityDateColumn == null) {
			return;
		}

		if (!bucket.has(event, validityDateColumn)) {
			return;
		}

		final CDateRange value = bucket.getAsDateRange(event, validityDateColumn);

		if (value.isOpen()) {
			return;
		}


		set.maskedAdd(value, dateRestriction);
	}

	@Override
	public Long createAggregationResult() {

		queryDateAggregator
				.map(Aggregator::createAggregationResult)
				.ifPresent(
						set::retainAll
				);

		return set.countDays();
	}

	@Override
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}

}
