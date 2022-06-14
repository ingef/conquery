package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.ToString;

/**
 * Aggregator summing over {@code addendColumn} and subtracting over {@code subtrahendColumn}, for money columns.
 */
@ToString(of = {"addendColumn", "subtrahendColumn"})
public class MoneyDiffSumAggregator extends ColumnAggregator<Long> {

	@Getter
	private final Column addendColumn;
	@Getter
	private final Column subtrahendColumn;
	private long sum;
	private boolean hit;

	public MoneyDiffSumAggregator(Column addend, Column subtrahend) {
		addendColumn = addend;
		subtrahendColumn = subtrahend;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		hit = false;
		sum = 0;
	}


	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getAddendColumn(), getSubtrahendColumn()};
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {

		if (!bucket.has(event, getAddendColumn()) && !bucket.has(event, getSubtrahendColumn())) {
			return;
		}

		hit = true;

		long addend = bucket.has(event, getAddendColumn()) ? bucket.getMoney(event, getAddendColumn()) : 0;

		long subtrahend = bucket.has(event, getSubtrahendColumn()) ? bucket.getMoney(event, getSubtrahendColumn()) : 0;

		sum = sum + addend - subtrahend;
	}

	@Override
	public Long createAggregationResult() {
		return hit ? sum : null;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.MoneyT.INSTANCE;
	}
}
