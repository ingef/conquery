package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.Getter;

public class IntegerDiffSumAggregator extends ColumnAggregator<Long> {

	@Getter
	private Column addendColumn;
	@Getter
	private Column subtrahendColumn;
	private long sum;

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
	public void aggregateEvent(Block block, int event) {
		long addend = block.has(event, getAddendColumn()) ? block.getInteger(event, getAddendColumn()) : 0;
		long subtrahend = block.has(event, getSubtrahendColumn()) ? block.getInteger(event, getSubtrahendColumn()) : 0;

		sum = sum + addend - subtrahend;
	}

	@Override
	public Long getAggregationResult() {
		return sum;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
