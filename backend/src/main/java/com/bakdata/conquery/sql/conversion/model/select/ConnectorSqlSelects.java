package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ConnectorSqlSelects {

	@Singular
	List<SqlSelect> preprocessingSelects;

	/**
	 * ConceptColumnSelect is an edge case which requires the connector column to be present in the event-filter step, but not afterward.
	 */
	@Builder.Default
	Optional<SqlSelect> connectorColumn = Optional.empty();

	// Empty if only used in event filter
	@Singular
	List<SqlSelect> aggregationSelects;

	// Selects that are applied on the aggregated validity date.
	@Singular
	List<SqlSelect> eventDateSelects;

	// Empty if only used in aggregation select
	@Singular
	List<SqlSelect> finalSelects;

	// An additional predecessor these SqlSelects require
	@Builder.Default
	Optional<QueryStep> additionalPredecessor = Optional.empty();

	public static ConnectorSqlSelects none() {
		return ConnectorSqlSelects.builder().build();
	}

}
