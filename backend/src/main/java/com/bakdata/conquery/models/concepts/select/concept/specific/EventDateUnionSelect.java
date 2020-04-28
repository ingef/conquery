package com.bakdata.conquery.models.concepts.select.concept.specific;

import java.util.stream.Collectors;

import c10n.C10N;
import com.bakdata.conquery.internationalization.EventDateUnionSelectC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;

@CPSType(id = "EVENT_DATE_UNION", base = Select.class)
public class EventDateUnionSelect extends UniversalSelect {
	
	
	public EventDateUnionSelect() {
		// This is just a workaround so that the validator is satisfied when a query is resolved. The effective label respects the provided locale
		this.setName("event-date");
		this.setLabel(C10N.get(EventDateUnionSelectC10n.class).label());
	}

	@Override
	public String getLabel() {
		return C10N.get(EventDateUnionSelectC10n.class, I18n.LOCALE.get()).label();
	}
	@Override
	public String getName() {
		return "event-date";
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new EventDateUnionAggregator(this.getHolder().findConcept().getConnectors().stream().map(Connector::getTable).map(Table::getId).collect(Collectors.toSet()));
	}
}
