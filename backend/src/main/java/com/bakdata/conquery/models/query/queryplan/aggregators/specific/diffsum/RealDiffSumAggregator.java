package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import lombok.Getter;
import lombok.ToString;

/**
 * Aggregator summing over {@code addendColumn} and subtracting over {@code subtrahendColumn}, for real columns.
 */
@ToString(callSuper = false, of = {"addendColumn", "subtrahendColumn"})
public class RealDiffSumAggregator extends ColumnAggregator<Double> {

	@Getter
	private final Column addendColumn;
	@Getter
	private final Column subtrahendColumn;

	private double sum;
	private boolean hit;

	public RealDiffSumAggregator(Column addend, Column subtrahend) {
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

		double addend = bucket.has(event, getAddendColumn())
								? bucket.getReal(event, getAddendColumn())
								: 0;

		double subtrahend = bucket.has(event, getSubtrahendColumn())
									? bucket.getReal(event, getSubtrahendColumn())
									: 0;

		sum = sum + addend - subtrahend;
	}

	@Override
	public Double createAggregationResult() {
		return hit ? sum : null;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NumericT.INSTANCE;
	}
}
