package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.QuarterAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Output first, last or random Year-Quarter in time
 */
@CPSType(id = "QUARTER", base = Select.class)
@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class QuarterSelect extends Select {

	@NotNull
	private final TemporalSampler sample;

	@Override
	public Aggregator<?> createAggregator() {
		return new QuarterAggregator(sample);
	}
}
