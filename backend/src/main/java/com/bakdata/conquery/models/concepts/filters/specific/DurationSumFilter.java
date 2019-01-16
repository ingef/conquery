package com.bakdata.conquery.models.concepts.filters.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQIntegerRangeFilter;
import com.bakdata.conquery.models.query.filter.AndFilterNode;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.filter.event.BeginsInRangeFilterNode;
import com.bakdata.conquery.models.query.filter.event.EndsInRangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregatorNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.MajorTypeId;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.EnumSet;

@Getter
@Setter
@Slf4j
@CPSType(id = "DURATION_SUM", base = Filter.class)
public class DurationSumFilter extends SingleColumnFilter<CQIntegerRangeFilter> {

	public static enum View {
		Time, Begin, End
	}

	public View view = View.Time;

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE_RANGE);
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		switch (getColumn().getType()) {
			case DATE:
			case DATE_RANGE: {
				f.setType(FEFilterType.INTEGER_RANGE);
				f.setMin(0);
				return;
			}
			default:
				throw new ConceptConfigurationException(getConnector(), "DURATION_SUM filter is incompatible with columns of type " + getColumn().getType());
		}
	}

	@Override
	public FilterNode createAggregator(CQIntegerRangeFilter filterValue) {
		if (view == View.Time)
			return new RangeFilterNode(this, filterValue, new DurationSumAggregatorNode(getColumn()));

		FilterNode<FilterValue.CQIntegerRangeFilter, DurationSumFilter> eventFilter =
				view == View.Begin ?
						new BeginsInRangeFilterNode(this, filterValue)
						: new EndsInRangeFilterNode(this, filterValue);


		return new AndFilterNode(
				this,
				Arrays.asList(
						eventFilter,
						new RangeFilterNode(this, filterValue, new DurationSumAggregatorNode(getColumn()))
				)
		);
	}
}
