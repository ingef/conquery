package com.bakdata.conquery.sql.model.operation;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;

public record CountAggregation(
		@NotNull @Valid ResolvedColumn column,
		@NotNull List<@NotNull @Valid ResolvedColumn> distinctBy
) implements ResolvedAggregation {

	public CountAggregation {
		distinctBy = ModelNormalization.immutableCopy(distinctBy);
	}

	@AssertTrue(message = "count columns must belong to the same table")
	public boolean isOnSameTable() {
		return AggregationValidation.sameTable(AggregationValidation.withColumn(column, distinctBy));
	}
}
