package com.bakdata.conquery.models.concepts.select.concept.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.UniversalAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.date.CountQuartersAggregator;
import lombok.NoArgsConstructor;

/**
 * Entity is included when the number of distinct quarters for all events is within a given range.
 * Implementation is specific for DateRanges
 */
@CPSType(id = "COUNT_QUARTERS", base = Select.class)
@NoArgsConstructor
public class CountQuartersSelect extends UniversalSelect {
	@Override
	public UniversalAggregator<?> createAggregator() {
		return new CountQuartersAggregator();
	}
}
