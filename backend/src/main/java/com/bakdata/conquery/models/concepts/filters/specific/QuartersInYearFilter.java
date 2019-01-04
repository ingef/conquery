package com.bakdata.conquery.models.concepts.filters.specific;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.ComplexFilter;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.aggregators.filter.QuartersInYearFilterNode;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQIntegerRangeFilter;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@CPSType(id="QUARTERS_IN_YEAR", base= Filter.class)
public class QuartersInYearFilter extends ComplexFilter<CQIntegerRangeFilter> {
	
	private static final long serialVersionUID = 1L;

	@Valid @NotNull @IdReference
	private Column column;
	
	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getColumn()};
	}
	
	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.INTEGER_RANGE);
		f.setMin(1);
		f.setMax(4);
	}


	@Override
	public FilterNode createAggregator(CQIntegerRangeFilter filterValue) {
		return new QuartersInYearFilterNode(this, filterValue);
	}

}
