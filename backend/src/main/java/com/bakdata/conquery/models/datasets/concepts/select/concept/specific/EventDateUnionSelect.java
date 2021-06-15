package com.bakdata.conquery.models.datasets.concepts.select.concept.specific;

import java.util.stream.Collectors;

import c10n.C10N;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;

/**
 * Collects the event dates that are corresponding to an enclosing {@link Connector} or {@link Concept} provided in a query.
 * The resulting date set is in bounds of a provided date restriction.
 */
@CPSType(id = "EVENT_DATE_UNION", base = Select.class)
public class EventDateUnionSelect extends UniversalSelect {

	{
		/*
		 *  WORKAROUND to satisfy the validator when the concept is added and a submitted query is deserialized.
		 *  The label is here internationalized but that doesn't have an effect on the effective label, which is used in a result header.
		 *  For the result header, the label is generated on a per request basis with respect to the request provided locale.
		 */
		this.setLabel(C10N.get(ResultHeadersC10n.class).dates());
	}

	@Override
	public String getLabel() {
		return C10N.get(ResultHeadersC10n.class, I18n.LOCALE.get()).dates();
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new EventDateUnionAggregator(getHolder().findConcept()
													   .getConnectors()
													   .stream()
													   .map(Connector::getTable)
													   .collect(Collectors.toSet()));
	}
}
