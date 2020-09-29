package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;

/**
 * Aggregator summing over {@code addendColumn} and subtracting over {@code subtrahendColumn}, for integer columns.
 */
public class IntegerDiffSumAggregator extends ColumnAggregator<Long> {

	@Getter
	private Column addendColumn;
	@Getter
	private Column subtrahendColumn;
	private long sum;
	private boolean hit;

	public IntegerDiffSumAggregator(Column addend, Column subtrahend) {
		this.addendColumn = addend;
		this.subtrahendColumn = subtrahend;
		this.sum = 0L;
	}

	@Override
	public IntegerDiffSumAggregator doClone(CloneContext ctx) {
		return new IntegerDiffSumAggregator(getAddendColumn(), getSubtrahendColumn());
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

		long addend = bucket.has(event, getAddendColumn()) ? bucket.getInteger(event, getAddendColumn()) : 0;
		long subtrahend = bucket.has(event, getSubtrahendColumn()) ? bucket.getInteger(event, getSubtrahendColumn()) : 0;

		sum = sum + addend - subtrahend;
	}

	@Override
	public Long getAggregationResult() {
		return hit ? sum : null;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
