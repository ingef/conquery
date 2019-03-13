package com.bakdata.conquery.models.concepts.filters.specific;

import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter
@Setter
@CPSType(id = "COUNT", base = Filter.class)
public class CountFilter extends SingleColumnFilter<Range.LongRange> {


	private boolean distinct;

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DECIMAL, MajorTypeId.INTEGER, MajorTypeId.REAL, MajorTypeId.STRING);
	}

	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.INTEGER_RANGE);
		f.setMin(1);
	}

	@Override
	public FilterNode createAggregator(Range.LongRange value) {
		if (distinct) {
			return new RangeFilterNode(value, new DistinctValuesWrapperAggregatorNode(new CountAggregator(getColumn()), getColumn()));
		}
		else {
			return new RangeFilterNode(value, new CountAggregator(getColumn()));
		}
	}
}
