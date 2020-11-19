package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;

/**
 * Aggregator summing over {@code addendColumn} and subtracting over {@code subtrahendColumn}, for real columns.
 */
public class RealDiffSumAggregator extends ColumnAggregator<Double> {

	@Getter
	private Column addendColumn;
	@Getter
	private Column subtrahendColumn;

	private double sum = 0;

	public RealDiffSumAggregator(Column addend, Column subtrahend) {
		this.addendColumn = addend;
		this.subtrahendColumn = subtrahend;
	}

	@Override
	public RealDiffSumAggregator doClone(CloneContext ctx) {
		return new RealDiffSumAggregator(getAddendColumn(), getSubtrahendColumn());
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

		setHit();

		double addend = bucket.has(event, getAddendColumn())
								? bucket.getReal(event, getAddendColumn())
								: 0;

		double subtrahend = bucket.has(event, getSubtrahendColumn())
									? bucket.getReal(event, getSubtrahendColumn())
									: 0;

		sum = sum + addend - subtrahend;
	}

	@Override
	public Double doGetAggregationResult() {
		return sum;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NUMERIC;
	}
}
