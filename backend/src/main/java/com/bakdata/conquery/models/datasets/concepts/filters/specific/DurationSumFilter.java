package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.EnumSet;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@CPSType(id = "DURATION_SUM", base = Filter.class)
public class DurationSumFilter extends SingleColumnFilter<Range.LongRange> {

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE_RANGE);
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		if (getColumn().getType() != MajorTypeId.DATE_RANGE) {
			throw new ConceptConfigurationException(getConnector(), "DURATION_SUM filter is incompatible with columns of type "
																	+ getColumn().getType());
		}

		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
		f.setMin(0);
	}

	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		return new RangeFilterNode(value, new DurationSumAggregator(getColumn()));
	}
}
