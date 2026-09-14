package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/** Groups select expressions by the concept-connector CTE phase that consumes them. */
@Value
@Builder
public class ConnectorSqlSelects {

	@Singular
	List<SqlSelect> preprocessingSelects;

	/** Empty when the select is used only for event filtering during preprocessing. */
	@Singular
	List<SqlSelect> aggregationSelects;

	/** Selects applied to the aggregated validity date. */
	@Singular
	List<SqlSelect> eventDateSelects;

	/** Empty when the select is used only during aggregation. */
	@Singular
	List<SqlSelect> finalSelects;

	/** An additional query-step predecessor required by these selects. */
	@Builder.Default
	Optional<QueryStep> additionalPredecessor = Optional.empty();

	public static ConnectorSqlSelects none() {
		return ConnectorSqlSelects.builder().build();
	}
}
