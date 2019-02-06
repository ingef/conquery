package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import lombok.Getter;

import java.math.BigDecimal;

public class DecimalDiffSumAggregator extends ColumnAggregator<BigDecimal> {

	@Getter
	private Column addendColumn;
	@Getter
	private Column subtrahendColumn;
	private BigDecimal sum = BigDecimal.ZERO;

	public DecimalDiffSumAggregator(SelectId id, Column addend, Column subtrahend) {
		super(id);
		this.addendColumn = addend;
		this.subtrahendColumn = subtrahend;
	}

	@Override
	public DecimalDiffSumAggregator clone() {
		return new DecimalDiffSumAggregator(getId(), getAddendColumn(), getSubtrahendColumn());
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getAddendColumn(), getSubtrahendColumn()};
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		BigDecimal addend = block.has(event, getAddendColumn()) ? block.getDecimal(event, getAddendColumn()) : BigDecimal.ZERO;

		BigDecimal subtrahend = block.has(event, getSubtrahendColumn()) ? block.getDecimal(event, getSubtrahendColumn()) : BigDecimal.ZERO;

		sum = sum.add(addend.subtract(subtrahend));
	}

	@Override
	public BigDecimal getAggregationResult() {
		return sum;
	}
}
