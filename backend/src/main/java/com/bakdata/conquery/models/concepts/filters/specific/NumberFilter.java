package com.bakdata.conquery.models.concepts.filters.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.filter.event.number.*;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This filter represents a filter on an integer columnof each event.
 */
@Getter
@Setter
@Slf4j
@CPSType(id = "NUMBER", base = Filter.class)
public class NumberFilter extends SingleColumnFilter<FilterValue<? extends IRange<? extends Number, ?>>> {

	

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		Column column = getColumn();
		switch (column.getType()) {
			case MONEY: //see #170  introduce money filter into frontend
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
		return new Column[]{getColumn()};
	}

	@Override
	public NumberFilterNode createAggregator(FilterValue<? extends IRange<? extends Number, ?>> filterValue) {
		switch (getColumn().getType()) {
			case MONEY:
				return new MoneyFilterNode(this, (FilterValue.CQIntegerRangeFilter) filterValue);
			case INTEGER:
				return new IntegerFilterNode(this, (FilterValue<Range.LongRange>) filterValue);
			case DECIMAL:
				return new DecimalFilterNode(this, (FilterValue<Range<BigDecimal>>) filterValue);
			case REAL:
				return new RealFilterNode(this, (FilterValue<Range.DoubleRange>) filterValue);
			default:
				throw new IllegalStateException(String.format("Column type %s may not be used (Assignment should not have been possible)", getColumn()));
		}
	}
}
