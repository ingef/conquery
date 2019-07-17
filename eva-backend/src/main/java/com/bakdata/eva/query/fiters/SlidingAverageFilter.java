package com.bakdata.eva.query.fiters;

import java.math.BigDecimal;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.validators.RequiresColumnType;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.eva.query.aggregators.SlidingAverageAggregator;

import lombok.Getter;

@Getter
@CPSType(id = "SLIDING_AVERAGE", base = Filter.class)
public class SlidingAverageFilter extends Filter<Range<BigDecimal>> {

	private static final long serialVersionUID = 1L;

	@Valid
	@NotNull
	@RequiresColumnType(MajorTypeId.DATE_RANGE)
	@NsIdRef
	protected Column dateRangeColumn;
	@Valid
	@NotNull
	@RequiresColumnType(MajorTypeId.REAL)
	@NsIdRef
	protected Column valueColumn;
	@Valid
	@NotNull
	@RequiresColumnType(MajorTypeId.INTEGER)
	@NsIdRef
	protected Column maximumDaysColumn;

	@Override
	public FilterNode<?> createAggregator(Range<BigDecimal> filterValue) {
		return new RangeFilterNode<>(Range.DoubleRange.fromNumberRange(filterValue), new SlidingAverageAggregator(dateRangeColumn, valueColumn, maximumDaysColumn));
	}

	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.REAL_RANGE);
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{dateRangeColumn, valueColumn, maximumDaysColumn};
	}
}
