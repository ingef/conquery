package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FEFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter
@Setter
@CPSType(id = "COUNT", base = Filter.class)
public class CountFilter extends Filter<Range.LongRange> {

	@Valid
	@NotNull
	@Getter
	@Setter
	@NsIdRef
	private Column column;

	private boolean distinct;


	@Valid
	@Getter
	@Setter
	@NsIdRefCollection
	private List<Column> distinctByColumn;


	@Override
	public void configureFrontend(FEFilterConfiguration.Top f) {
		f.setType(FEFilterType.Fields.INTEGER_RANGE);
		f.setMin(1);
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		if (distinct) {
			return new RangeFilterNode(value, new DistinctValuesWrapperAggregator(new CountAggregator(getColumn()), DistinctValuesWrapperAggregator.fallback(getDistinctByColumn(), getColumn())));
		}
		return new RangeFilterNode(value, new CountAggregator(getColumn()));
	}

	@Override
	public Column[] getRequiredColumns() {
		List<Column> out = new ArrayList<>();

		out.add(getColumn());

		if (distinctByColumn != null){
			out.addAll(getDistinctByColumn());
		}

		return out.toArray(Column[]::new);
	}
}
