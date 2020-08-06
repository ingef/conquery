package com.bakdata.conquery.models.concepts.select.concept.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.UniversalAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.date.DateUnionAggregator;

@CPSType(id = "DATE_UNION", base = Select.class)
public class DateUnionSelect extends UniversalSelect {

	@Override
	public UniversalAggregator<?> createAggregator() {
		return new DateUnionAggregator();
	}
}
