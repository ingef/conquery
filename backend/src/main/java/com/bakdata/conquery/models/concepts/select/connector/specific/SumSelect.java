package com.bakdata.conquery.models.concepts.select.connector.specific;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.externalservice.ResultType;
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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "SUM", base = Select.class)
public class SumSelect extends Select {

	private boolean distinct = false;

	@NsIdRef
	@NotNull
	private Column column;

	@NsIdRef
	private Column subtractColumn;

	@Override
	protected Aggregator<?> createAggregator() {
		if (distinct) {
			return new DistinctValuesWrapperAggregatorNode(getAggregator(), getColumn());
		}
		else {
			return getAggregator();
		}
	}
	private ColumnAggregator<?> getAggregator() {
		if (subtractColumn == null) {
			switch (getColumn().getType()) {
				case INTEGER:
					return new IntegerSumAggregator(getColumn());
				case MONEY:
					return new MoneySumAggregator(getColumn());
				case DECIMAL:
					return new DecimalSumAggregator(getColumn());
				case REAL:
					return new RealSumAggregator(getColumn());
				default:
					throw new IllegalStateException(String.format("Invalid column type '%s' for SUM Aggregator", getColumn().getType()));
			}
		}
		else {
			if(getColumn().getType() != getSubtractColumn().getType()) {
				throw new IllegalStateException(String.format("Column types are not the same: Column %s\tSubstractColumn %s", getColumn().getType(), getSubtractColumn().getType()));
			}
			switch (getColumn().getType()) {
				case INTEGER:
					return new IntegerDiffSumAggregator(getColumn(), getSubtractColumn());
				case MONEY:
					return new MoneyDiffSumAggregator(getColumn(), getSubtractColumn());
				case DECIMAL:
					return new DecimalDiffSumAggregator(getColumn(), getSubtractColumn());
				case REAL:
					return new RealDiffSumAggregator(getColumn(), getSubtractColumn());
				default:
					throw new IllegalStateException(String.format("Invalid column type '%s' for SUM Aggregator", getColumn().getType()));
			}
		}
	}
	

	
	@Override
	public ResultType getResultType() {
		switch (getColumn().getType()) {
			case INTEGER:
				return ResultType.INTEGER;
			case MONEY:
				return ResultType.MONEY;
			case DECIMAL:
			case REAL:
				return ResultType.NUMERIC;
			default:
				throw new IllegalStateException(String.format("Invalid column type '%s' for Aggregator", getColumn().getType()));
		}
	}
}
