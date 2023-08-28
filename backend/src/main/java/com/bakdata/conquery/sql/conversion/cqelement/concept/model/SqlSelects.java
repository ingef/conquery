package com.bakdata.conquery.sql.conversion.cqelement.concept.model;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class SqlSelects {
	// Preprocessing is _always_ required, and if it is only the reference to an existing column
	@NonNull
	List<ConquerySelect> forPreprocessingStep;
	// Empty if only used in event filter
	@Builder.Default
	List<ConquerySelect> forGroupByStep = Collections.emptyList();
	// Empty if only used in group by
	@Builder.Default
	List<ConquerySelect> forGroupFilterStep = Collections.emptyList();
}
