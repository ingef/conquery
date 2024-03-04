package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;
import java.util.Optional;

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
	// An additional predecessor these SqlSelects require
	@Builder.Default
	Optional<QueryStep> additionalPredecessor = Optional.empty();
}
