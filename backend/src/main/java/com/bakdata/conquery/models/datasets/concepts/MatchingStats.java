package com.bakdata.conquery.models.datasets.concepts;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.common.daterange.CDateRange;

public interface MatchingStats {

	long countEvents();

	long countEntities();

	@Nullable
	CDateRange spanEvents();

}
