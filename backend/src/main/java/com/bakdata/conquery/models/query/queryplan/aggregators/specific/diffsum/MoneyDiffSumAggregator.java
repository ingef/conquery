package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;

/**
 * Aggregator summing over {@code addendColumn} and subtracting over {@code subtrahendColumn}, for money columns.
 */
public class MoneyDiffSumAggregator extends ColumnAggregator<Long> {

	@Getter
	private Column addendColumn;
	@Getter
	private Column subtrahendColumn;
	private long sum = 0L;
	private boolean hit;

	public MoneyDiffSumAggregator(Column addend, Column subtrahend) {
		this.addendColumn = addend;
		this.subtrahendColumn = subtrahend;
	}

	@Override
	public MoneyDiffSumAggregator doClone(CloneContext ctx) {
		return new MoneyDiffSumAggregator(getAddendColumn(), getSubtrahendColumn());
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
	public Long getAggregationResult() {
		return hit ? sum : null;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.MONEY;
	}
}
