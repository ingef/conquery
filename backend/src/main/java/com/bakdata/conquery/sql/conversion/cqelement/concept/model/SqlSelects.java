package com.bakdata.conquery.sql.conversion.cqelement.concept.model;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class SqlSelects {
	@Builder.Default
	List<SqlSelect> forPreprocessingStep = Collections.emptyList();
	// Empty if only used in event filter
	@Builder.Default
	List<SqlSelect> forAggregationSelectStep = Collections.emptyList();
	// Empty if only used in aggregation select
	@Builder.Default
	List<SqlSelect> forFinalStep = Collections.emptyList();
}
