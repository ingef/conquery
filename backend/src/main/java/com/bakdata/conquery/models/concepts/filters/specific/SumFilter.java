package com.bakdata.conquery.models.concepts.filters.specific;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.GroupFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.aggregators.filter.diffsum.DecimalDiffSumFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.diffsum.IntegerDiffSumFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.diffsum.MoneyDiffSumFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.diffsum.RealDiffSumFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.sum.DecimalSumFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.sum.IntegerSumFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.sum.MoneySumFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.sum.RealSumFilterNode;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
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
public class SumFilter extends GroupFilter<FilterValue<? extends IRange<?, ?>>> {

	private static final long serialVersionUID = 1L;

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
			case MONEY: //TODO introduce money filter into frontend
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

	@Override
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
		if (getSubtractColumn() == null) {
			switch (getColumn().getType()) {
				case MONEY:
					return new MoneySumFilterNode(this, filterValue);
				case INTEGER:
					return new IntegerSumFilterNode(this, filterValue);
				case DECIMAL:
					return new DecimalSumFilterNode(this, filterValue);
				case REAL:
					return new RealSumFilterNode(this, filterValue);
				default:
					throw new IllegalStateException("No Sum Filter for type " + getColumn().getType().name());
			}
		}
		else {
			switch (getColumn().getType()) {
				case MONEY:
					return new MoneyDiffSumFilterNode(this, filterValue);
				case INTEGER:
					return new IntegerDiffSumFilterNode(this, filterValue);
				case DECIMAL:
					return new DecimalDiffSumFilterNode(this, filterValue);
				case REAL:
					return new RealDiffSumFilterNode(this, filterValue);
				default:
					throw new IllegalStateException("No Sum Filter for type " + getColumn().getType().name());
			}
		}
	}
}
