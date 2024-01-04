package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.ToString;

/**
 * Aggregator summing over {@code addendColumn} and subtracting over {@code subtrahendColumn}, for integer columns.
 */
@ToString(callSuper = false, of = {"addendColumn", "subtrahendColumn"})
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
	public void init(Entity entity, QueryExecutionContext context) {
		hit = false;
		sum = 0;
	}


	@Override
	public List<Column> getRequiredColumns() {
		final List<Column> out = new ArrayList<>();

		out.add(getAddendColumn());
		out.add(getSubtrahendColumn());

		return out;
	}
	@Override
	public void consumeEvent(Bucket bucket, int event) {

		if (!bucket.has(event, getAddendColumn()) && !bucket.has(event, getSubtrahendColumn())) {
			return;
		}

		hit = true;

		long addend = bucket.has(event, getAddendColumn()) ? bucket.getInteger(event, getAddendColumn()) : 0;
		long subtrahend = bucket.has(event, getSubtrahendColumn()) ? bucket.getInteger(event, getSubtrahendColumn()) : 0;

		sum = sum + addend - subtrahend;
	}

	@Override
	public Long createAggregationResult() {
		return hit ? sum : null;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}
