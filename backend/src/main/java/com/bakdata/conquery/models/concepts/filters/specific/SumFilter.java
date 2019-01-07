package com.bakdata.conquery.models.concepts.filters.specific;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.DecimalDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.IntegerDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.MoneyDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.RealDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.DecimalSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.IntegerSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.MoneySumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.RealSumAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This filter represents a filter on the sum of one integer column.
 */
@Getter
@Setter
@Slf4j
@CPSType(id = "SUM", base = Filter.class)
public class SumFilter extends Filter<FilterValue<? extends IRange<?, ?>>> {

	

	@Valid
	@NotNull
	@Getter
	@Setter
	@IdReference
	private Column column;
	@Valid
	@Getter
	@Setter
	@IdReference
	private Column subtractColumn;

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		Column column = getColumn();
		switch (column.getType()) {
			case MONEY: //see #171  introduce money filter into frontend
			case INTEGER: {
				f.setType(FEFilterType.INTEGER_RANGE);
				return;
			}
			case DECIMAL:
			case REAL: {
				f.setType(FEFilterType.REAL_RANGE);
				return;
			}
			default:
				throw new ConceptConfigurationException(getConnector(), "NUMBER filter is incompatible with columns of type " + column.getType());
		}
	}

	public Column[] getRequiredColumns() {
		if (getSubtractColumn() == null) {
			return new Column[]{getColumn()};
		}
		else {
			return new Column[]{getColumn(), getSubtractColumn()};
		}
	}

	@Override
	public FilterNode createAggregator(FilterValue<? extends IRange<?, ?>> filterValue) {
		return new RangeFilterNode(this, filterValue, getAggregator(filterValue));
	}

	private Aggregator<?> getAggregator(FilterValue<? extends IRange<?, ?>> filterValue) {
		if (getSubtractColumn() == null) {
			switch (getColumn().getType()) {
				case MONEY:
					return new MoneySumAggregator(getColumn());
				case INTEGER:
					return new IntegerSumAggregator(getColumn());
				case DECIMAL:
					return new DecimalSumAggregator(getColumn());
				case REAL:
					return new RealSumAggregator(getColumn());
				default:
					throw new IllegalStateException("No Sum Filter for type " + getColumn().getType().name());
			}
		}
		else {
			switch (getColumn().getType()) {
				case MONEY:
					return new MoneyDiffSumAggregator(getColumn(), getSubtractColumn());
				case INTEGER:
					return new IntegerDiffSumAggregator(getColumn(), getSubtractColumn());
				case DECIMAL:
					return new DecimalDiffSumAggregator(getColumn(), getSubtractColumn());
				case REAL:
					return new RealDiffSumAggregator(getColumn(), getSubtractColumn());
				default:
					throw new IllegalStateException("No Sum Filter for type " + getColumn().getType().name());
			}
		}
	}
}
