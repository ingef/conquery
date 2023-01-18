package com.bakdata.conquery.models.datasets.concepts.select.concept.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDurationSumAggregator;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@CPSType(id = "EVENT_DURATION_SUM", base = Select.class)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventDurationSumSelect extends UniversalSelect {


	@Override
	public Aggregator<?> createAggregator() {
		return new EventDurationSumAggregator();
	}

	public static EventDurationSumSelect create(String name) {
		Preconditions.checkArgument(StringUtils.isNotBlank(name), "The name of the select must not be blank");
		EventDurationSumSelect select = new EventDurationSumSelect();
		select.setName(name);
		return select;
	}
}
