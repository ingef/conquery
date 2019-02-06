package com.bakdata.conquery.models.concepts.select.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregatorNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.DecimalDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.IntegerDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.MoneyDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.RealDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.DecimalSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.IntegerSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.MoneySumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.RealSumAggregator;
import com.bakdata.conquery.models.query.select.Select;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@CPSType(id = "SUM", base = Select.class)
public class SumSelect extends Select {

	private boolean distinct = false;

	@Getter
	@IdReference
	@NotNull
	private Column column;

	@Getter
	@IdReference
	private Column subtractColumn;

	@Override
	public Aggregator<?> createAggregator() {
		if (distinct)
			return new DistinctValuesWrapperAggregatorNode((ColumnAggregator) getAggregator(), getColumn());
		else
			return getAggregator();
	}

	private Aggregator<?> getAggregator() {
		if (subtractColumn == null) {
			switch (getColumn().getType()) {
				case INTEGER:
					return new IntegerSumAggregator(getId(), getColumn());
				case MONEY:
					return new MoneySumAggregator(getId(), getColumn());
				case DECIMAL:
					return new DecimalSumAggregator(getId(), getColumn());
				case REAL:
					return new RealSumAggregator(getId(), getColumn());
				default:
					throw new IllegalStateException(String.format("Invalid column type '%s' for SUM Aggregator", getColumn().getType()));
			}
		}
		else {
			switch (getColumn().getType()) {
				case INTEGER:
					return new IntegerDiffSumAggregator(getId(), getColumn(), getSubtractColumn());
				case MONEY:
					return new MoneyDiffSumAggregator(getId(), getColumn(), getSubtractColumn());
				case DECIMAL:
					return new DecimalDiffSumAggregator(getId(), getColumn(), getSubtractColumn());
				case REAL:
					return new RealDiffSumAggregator(getId(), getColumn(), getSubtractColumn());
				default:
					throw new IllegalStateException(String.format("Invalid column type '%s' for SUM Aggregator", getColumn().getType()));
			}
		}
	}
}
