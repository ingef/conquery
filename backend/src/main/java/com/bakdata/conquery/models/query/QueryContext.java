package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.datasets.Column;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

@Getter @AllArgsConstructor @NoArgsConstructor
public class QueryContext {

	@Wither
	private Column validityDateColumn;
}
