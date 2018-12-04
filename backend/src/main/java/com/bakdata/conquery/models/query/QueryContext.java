package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.datasets.Column;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

@Getter @AllArgsConstructor @NoArgsConstructor
@Wither
public class QueryContext {

	private Column validityDateColumn;
	private CDateRange dateRestriction;
}
