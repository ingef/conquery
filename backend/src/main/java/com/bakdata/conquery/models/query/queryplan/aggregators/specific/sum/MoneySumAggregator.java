package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import java.math.BigDecimal;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import lombok.ToString;

/**
 * Aggregator implementing a sum over {@code column}, for money columns.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class MoneySumAggregator extends SingleColumnAggregator<BigDecimal> {

	private boolean hit = false;
	private BigDecimal sum;

	public MoneySumAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		hit = false;
		sum = BigDecimal.ZERO;
	}


	@Override
	public void consumeEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		hit = true;

		final BigDecimal addend = bucket.getMoney(event, getColumn());

		sum = sum.add(addend);
	}

	@Override
	public BigDecimal createAggregationResult() {
		return hit ? sum : null;
	}

}
