package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.filter.event.number.DecimalFilterNode;
import com.bakdata.conquery.models.query.filter.event.number.IntegerFilterNode;
import com.bakdata.conquery.models.query.filter.event.number.MoneyFilterNode;
import com.bakdata.conquery.models.query.filter.event.number.RealFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
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
public class NumberFilter<RANGE extends IRange<? extends Number, ?>> extends SingleColumnFilter<RANGE> {

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		final String type = switch (getColumn().getType()) {
			case MONEY -> FrontendFilterType.Fields.MONEY_RANGE;
			case INTEGER -> FrontendFilterType.Fields.INTEGER_RANGE;
			case DECIMAL, REAL -> FrontendFilterType.Fields.REAL_RANGE;
			default -> throw new ConceptConfigurationException(getConnector(), "NUMBER filter is incompatible with columns of type " + getColumn().getType());
		};

		f.setType(type);
	}


	@Override
	public FilterNode<?> createFilterNode(RANGE value) {
		return switch (getColumn().getType()) {
			case MONEY -> new MoneyFilterNode(getColumn(), (Range.LongRange) value);
			case INTEGER -> new IntegerFilterNode(getColumn(), (Range.LongRange) value);
			case DECIMAL -> new DecimalFilterNode(getColumn(), ((Range<BigDecimal>) value));
			case REAL -> new RealFilterNode(getColumn(), Range.DoubleRange.fromNumberRange(value));

			default -> throw new IllegalStateException(String.format("Column type %s may not be used (Assignment should not have been possible)", getColumn()));
		};
	}
}
