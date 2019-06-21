package com.bakdata.eva.query.selects;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.eva.query.aggregators.SlidingSumAggregator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@CPSType(id = "SLIDING_SUM", base = Select.class)
public class SlidingSumSelect extends Select {

	@Getter
	@NsIdRef
	@NotNull
	private Column maximumDaysColumn;

	@Getter
	@NsIdRef
	@NotNull
	private Column dateRangeColumn;

	@Getter
	@NsIdRef
	@NotNull
	private Column valueColumn;


	@Override
	public Aggregator<?> createAggregator() {
		return new SlidingSumAggregator(dateRangeColumn, valueColumn, maximumDaysColumn);
	}
}
