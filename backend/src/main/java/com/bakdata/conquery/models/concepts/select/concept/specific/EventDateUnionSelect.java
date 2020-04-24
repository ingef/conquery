package com.bakdata.conquery.models.concepts.select.concept.specific;

import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;

@CPSType(id = "EVENT_DATE_UNION", base = Select.class)
public class EventDateUnionSelect extends UniversalSelect {


	@Override
	public Aggregator<?> createAggregator() {
		return new EventDateUnionAggregator(this.getHolder().findConcept().getConnectors().stream().map(Connector::getTable).map(Table::getId).collect(Collectors.toSet()));
	}
}
