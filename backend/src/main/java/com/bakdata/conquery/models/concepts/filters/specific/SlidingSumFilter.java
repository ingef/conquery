package com.bakdata.conquery.models.concepts.filters.specific;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.validators.RequiresColumnType;
import com.bakdata.conquery.models.query.aggregators.filter.SlidingSumFilterNode;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQRealRangeFilter;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="SLIDING_SUM", base=Filter.class)
public class SlidingSumFilter extends Filter<CQRealRangeFilter>{

	private static final long serialVersionUID = 1L;
	
	@Valid @NotNull @RequiresColumnType(MajorTypeId.DATE_RANGE) @IdReference
	protected Column dateRangeColumn;
	@Valid @NotNull @RequiresColumnType(MajorTypeId.REAL) @IdReference
	protected Column valueColumn;
	@Valid @NotNull @RequiresColumnType(MajorTypeId.INTEGER) @IdReference
	protected Column maximumDaysColumn;
	
	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.REAL_RANGE);
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[] {dateRangeColumn, valueColumn, maximumDaysColumn};
	}
	
	@Override
	public FilterNode createAggregator(CQRealRangeFilter filterValue) {
		return new SlidingSumFilterNode(this, filterValue);
	}
}
