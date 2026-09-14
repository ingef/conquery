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

	FieldWrapper<T> groupBy;

	QueryStep additionalPredecessor;

	public Optional<QueryStep> getAdditionalPredecessor() {
		return Optional.ofNullable(additionalPredecessor);
	}
}
