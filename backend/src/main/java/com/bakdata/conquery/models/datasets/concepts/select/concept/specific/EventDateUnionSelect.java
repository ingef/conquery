package com.bakdata.conquery.models.datasets.concepts.select.concept.specific;

import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.select.EventDateUnionSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;

/**
 * Collects the event dates that are corresponding to an enclosing {@link Connector} or {@link Concept} provided in a query.
 * The resulting date set is in bounds of a provided date restriction.
 */
@CPSType(id = "EVENT_DATE_UNION", base = Select.class)
public class EventDateUnionSelect extends UniversalSelect {

	@Override
	public Aggregator<?> createAggregator() {
		EventDateUnionAggregator dateUnionAggregator = new EventDateUnionAggregator();

		dateUnionAggregator.setRequiredTables(getHolder().findConcept()
														 .getConnectors()
														 .stream()
														 .map(Connector::getResolvedTable)
														 .collect(Collectors.toSet()));


		return dateUnionAggregator;
	}

	@Override
	public boolean isEventDateSelect() {
		return true;
	}

	@Override
	public SelectConverter<EventDateUnionSelect> createConverter() {
		return new EventDateUnionSelectConverter();
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE);
	}
}
