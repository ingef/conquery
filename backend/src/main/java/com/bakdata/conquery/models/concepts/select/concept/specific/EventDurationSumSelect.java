package com.bakdata.conquery.models.concepts.select.concept.specific;

import c10n.C10N;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDurationSumAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.EnumSet;

@CPSType(id = "EVENT_DURATION_SUM", base = Select.class)
public class EventDurationSumSelect extends UniversalSelect {

	{
		/*
		 *  WORKAROUND to satisfy the validator when the concept is added and a submitted query is deserialized.
		 *  The label is here internationalized but that doesn't have an effect on the effective label, which is used in a result header.
		 *  For the result header, the label is generated on a per request basis with respect to the request provided locale.
		 *  This sets also the default name under which this aggregator can be referenced.
		 */
		this.setName("event_duration_sum");
		this.setLabel(C10N.get(ResultHeadersC10n.class).eventDuration());
	}

	@Override
	public String getLabel() {
		return C10N.get(ResultHeadersC10n.class, I18n.LOCALE.get()).eventDuration();
	}


	@Override
	public Aggregator<?> createAggregator() {
		return new EventDurationSumAggregator();
	}
}
