package com.bakdata.conquery.models.concepts.filters.specific;

import java.math.BigDecimal;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.SimpleFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.aggregators.filter.number.DecimalFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.number.IntegerFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.number.MoneyFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.number.NumberFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.number.RealFilterNode;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;

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
public class NumberFilter extends SimpleFilter<FilterValue<? extends IRange<? extends Number, ?>>> {

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
		if (subtractColumn == null) {
			return new Column[]{getColumn()};
		}
		else {
			return new Column[]{getColumn(), getSubtractColumn()};
		}
	}

	@Override
	public NumberFilterNode createAggregator(FilterValue<? extends IRange<? extends Number, ?>> filterValue) {
		switch (getColumn().getType()) {
			case MONEY:
				return new MoneyFilterNode(this, (FilterValue<Range.LongRange>) filterValue);
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
