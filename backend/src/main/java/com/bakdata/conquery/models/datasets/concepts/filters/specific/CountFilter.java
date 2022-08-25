package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.apiv1.frontend.FEFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.NoArgsConstructor;

@CPSType(id = "COUNT", base = Filter.class)
@NoArgsConstructor
@Data
public class CountFilter extends Filter<Range.LongRange> {

	@Valid
	@NotEmpty
	@NsIdRefCollection
	private List<Column> column;

	private boolean distinct;

	@Override
	public void configureFrontend(FEFilterConfiguration.Top f) {
		f.setType(FEFilterType.Fields.INTEGER_RANGE);
		f.setMin(1);
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		if (distinct) {
			return new RangeFilterNode(value, new DistinctValuesWrapperAggregator(new CountAggregator(), getColumn()));
		}
		return new RangeFilterNode(value, new CountAggregator(getColumn().get(0)));
	}

	@Override
	public Column[] getRequiredColumns() {
		return getColumn().toArray(Column[]::new);
	}

	@JsonIgnore
	@ValidationMethod(message = "Cannot use multiple columns, when distinct is not set.")
	public boolean isMultiOnlyWhenDistinct() {
		if(!isDistinct()){
			return getColumn().size() == 1;
		}

		return true;
	}
}
