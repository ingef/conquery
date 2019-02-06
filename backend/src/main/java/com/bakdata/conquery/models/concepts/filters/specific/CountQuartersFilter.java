package com.bakdata.conquery.models.concepts.filters.specific;


import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQIntegerRangeFilter;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.MajorTypeId;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;

@Setter @Getter
@CPSType(id="COUNT_QUARTERS", base=Filter.class)
public class CountQuartersFilter extends SingleColumnFilter<CQIntegerRangeFilter> {
	
	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE, MajorTypeId.DATE_RANGE);
	}

	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.INTEGER_RANGE);
		f.setMin(1);
	}

	@Override
	public FilterNode createFilter(CQIntegerRangeFilter filterValue, Aggregator<?> aggregator) {
		if (getColumn().getType() == MajorTypeId.DATE_RANGE) {
			return new RangeFilterNode(this, filterValue, aggregator);
		}
		else {
			return new RangeFilterNode(this, filterValue, aggregator);
		}
	}
}
