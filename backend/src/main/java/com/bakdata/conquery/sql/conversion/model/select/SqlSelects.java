package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class SqlSelects {
	@Singular
	List<SqlSelect> preprocessingSelects;
	// Empty if only used in event filter
	@Singular
	List<SqlSelect> aggregationSelects;
	// Empty if only used in aggregation select
	@Singular
	List<SqlSelect> finalSelects;
	// Additional predecessors these SqlSelects require
	@Singular
	List<QueryStep> additionalPredecessors;
}
