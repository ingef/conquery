package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SingleColumnSqlSelect;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * Intermediate projections shared by aggregate select and filter compilation.
 *
 * @param <T> grouped aggregation result type
 */
@Value
@Builder
public class CommonAggregationSelect<T> {

	@Singular
	List<SingleColumnSqlSelect> rootSelects;

	/** Aggregate expression whose alias identifies the result, including results computed in a separate CTE. */
	FieldWrapper<T> groupBy;

	/** When present, this CTE already computes groupBy; do not also add it to the main aggregation selects. */
	QueryStep additionalPredecessor;

	public Optional<QueryStep> getAdditionalPredecessor() {
		return Optional.ofNullable(additionalPredecessor);
	}
}
