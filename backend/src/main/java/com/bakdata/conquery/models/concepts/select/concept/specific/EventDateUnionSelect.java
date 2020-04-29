package com.bakdata.conquery.models.concepts.select.concept.specific;

import java.util.stream.Collectors;

import c10n.C10N;
import com.bakdata.conquery.internationalization.EventDateUnionSelectC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;

/**
 * Collects the event dates that are corresponding to an enclosing {@link Connector} or {@link Concept} provided in a query.
 * The resulting date set is in bounds of a provided date restriction.
 * This works as a basic {@link Select} that is by default added to all {@link Concept}s and {@link Connector}s ({@link AdminProcessor#addConcept(Dataset, Concept)}).
 */
@CPSType(id = "EVENT_DATE_UNION", base = Select.class)
public class EventDateUnionSelect extends UniversalSelect {
	
	{
		/*
		 *  WORKAROUND to satisfy the validator when the concept is added and a submitted query is deserialized.
		 *  The label is here internationalized but that doesn't have an effect on the effective label, which is used in a result header.
		 *  For the result header, the label is generated on a per request basis with respect to the request provided locale.  
		 */
		this.setName("event-date");
		this.setLabel(C10N.get(EventDateUnionSelectC10n.class).label());		
	}
	
	@Override
	public String getLabel() {
		return C10N.get(EventDateUnionSelectC10n.class, I18n.LOCALE.get()).label();
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new EventDateUnionAggregator(this.getHolder().findConcept().getConnectors().stream().map(Connector::getTable).map(Table::getId).collect(Collectors.toSet()));
	}
}
