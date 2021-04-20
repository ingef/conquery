package com.bakdata.conquery.models.concepts.select.connector.specific;

import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.QuarterAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Entity is included when the the number of quarters with events is within a specified range.
 */
@CPSType(id = "QUARTER", base = Select.class)
public class QuarterSelect extends SingleColumnSelect {

	private final TemporalSampler sample;

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE, MajorTypeId.DATE_RANGE);
	}

	@JsonCreator
	public QuarterSelect(@NsIdRef Column column, TemporalSampler sample) {
		super(column);
		this.sample = sample;
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new QuarterAggregator(getColumn(), sample);
	}
}
