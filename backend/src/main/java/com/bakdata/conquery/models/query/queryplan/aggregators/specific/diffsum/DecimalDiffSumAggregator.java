package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import java.math.BigDecimal;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.Getter;

public class DecimalDiffSumAggregator extends ColumnAggregator<BigDecimal> {

	@Getter
	private Column addendColumn;
	@Getter
	private Column subtrahendColumn;
	private BigDecimal sum = BigDecimal.ZERO;

	public DecimalDiffSumAggregator(Column addend, Column subtrahend) {
		this.addendColumn = addend;
		this.subtrahendColumn = subtrahend;
	}

	@Override
	public DecimalDiffSumAggregator doClone(CloneContext ctx) {
		return new DecimalDiffSumAggregator(getAddendColumn(), getSubtrahendColumn());
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getAddendColumn(), getSubtrahendColumn()};
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		BigDecimal addend = bucket.has(event, getAddendColumn()) ? bucket.getDecimal(event, getAddendColumn()) : BigDecimal.ZERO;

		BigDecimal subtrahend = bucket.has(event, getSubtrahendColumn()) ? bucket.getDecimal(event, getSubtrahendColumn()) : BigDecimal.ZERO;

		sum = sum.add(addend.subtract(subtrahend));
	}

	@Override
	public BigDecimal getAggregationResult() {
		return sum;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NUMERIC;
	}
}
