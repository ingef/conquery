package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import lombok.Getter;

public class RealDiffSumAggregator extends ColumnAggregator<Double> {

	@Getter
	private Column addendColumn;
	@Getter
	private Column subtrahendColumn;

	private double sum = 0;

	public RealDiffSumAggregator(SelectId id, Column addend, Column subtrahend) {
		super(id);
		this.addendColumn = addend;
		this.subtrahendColumn = subtrahend;
	}

	@Override
	public RealDiffSumAggregator clone() {
		return new RealDiffSumAggregator(getId(), getAddendColumn(), getSubtrahendColumn());
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getAddendColumn(), getSubtrahendColumn()};
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		double addend = block.has(event, getAddendColumn())
								? block.getReal(event, getAddendColumn())
								: 0;

		double subtrahend = block.has(event, getSubtrahendColumn())
									? block.getReal(event, getSubtrahendColumn())
									: 0;

		sum = sum + addend - subtrahend;
	}

	@Override
	public Double getAggregationResult() {
		return sum;
	}
}
