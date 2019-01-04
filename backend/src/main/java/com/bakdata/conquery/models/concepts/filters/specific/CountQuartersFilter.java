package com.bakdata.conquery.models.concepts.filters.specific;


import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.GroupSingleColumnFilter;
import com.bakdata.conquery.models.query.aggregators.filter.CountQuartersOfDateRangeFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.CountQuartersOfDatesFilterNode;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQIntegerRangeFilter;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@CPSType(id="COUNT_QUARTERS", base=Filter.class)
public class CountQuartersFilter extends GroupSingleColumnFilter<CQIntegerRangeFilter> {
	
	private static final long serialVersionUID = 1L;
	
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
	public FilterNode createAggregator(CQIntegerRangeFilter filterValue) {
		if (getColumn().getType() == MajorTypeId.DATE_RANGE) {
			return new CountQuartersOfDateRangeFilterNode(this, filterValue);
		}
		else {
			return new CountQuartersOfDatesFilterNode(this, filterValue);
		}
	}
}
