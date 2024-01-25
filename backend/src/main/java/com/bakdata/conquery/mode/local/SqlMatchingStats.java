package com.bakdata.conquery.mode.local;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import lombok.Value;

@Value
public class SqlMatchingStats implements MatchingStats {

	long numberOfEvents;
	long numberOfEntities;
	CDateRange span;

	@Override
	public long countEvents() {
		return numberOfEvents;
	}

	@Override
	public long countEntities() {
		return numberOfEntities;
	}

	@Override
	public CDateRange spanEvents() {
		return span;
	}

}
