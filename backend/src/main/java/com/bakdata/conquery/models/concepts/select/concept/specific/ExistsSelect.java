package com.bakdata.conquery.models.concepts.select.concept.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;

@CPSType(id = "EXISTS", base = Select.class)
public class ExistsSelect extends UniversalSelect {

	@Override
	public ExistsAggregator createAggregator() {
		return new ExistsAggregator();
	}
}
