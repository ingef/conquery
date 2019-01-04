package com.bakdata.conquery.models.concepts.filters.specific;

import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.GroupSingleColumnFilter;
import com.bakdata.conquery.models.query.aggregators.filter.CountDistinctFilterNode;
import com.bakdata.conquery.models.query.aggregators.filter.CountFilterNode;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQIntegerRangeFilter;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter @Setter
@CPSType(id="COUNT", base=Filter.class)
public class CountFilter extends GroupSingleColumnFilter<CQIntegerRangeFilter> {

	private static final long serialVersionUID = 1L;
	
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
	public FilterNode createAggregator(CQIntegerRangeFilter filterValue) {
		if (distinct) {
			return new CountDistinctFilterNode(this, filterValue);
		}
		else {
			return new CountFilterNode(this, filterValue);
		}
	}
}
