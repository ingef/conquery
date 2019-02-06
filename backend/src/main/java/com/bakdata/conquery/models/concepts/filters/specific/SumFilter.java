package com.bakdata.conquery.models.concepts.filters.specific;

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
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregatorNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

	private boolean distinct = false;

	@Override
	public FilterNode createFilter(FilterValue<? extends IRange<?, ?>> filterValue, Aggregator<?> aggregator) {

		if (distinct)
			return new RangeFilterNode(this, filterValue, new DistinctValuesWrapperAggregatorNode((ColumnAggregator) aggregator, getColumn()));

		return new RangeFilterNode(this, filterValue, aggregator);
	}
}
