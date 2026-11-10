package com.bakdata.conquery.sql.model.operation;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.schema.DateColumns;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;

public record DurationSumAggregation(
		@NotNull @Valid DateColumns dates,
		@NotNull List<@NotNull @Valid ResolvedColumn> distinctBy
) implements ResolvedAggregation {

	public DurationSumAggregation {
		distinctBy = ModelNormalization.immutableCopy(distinctBy);
	}

	@AssertTrue(message = "duration-sum columns must belong to the same table")
	public boolean isOnSameTable() {
		return dates == null || AggregationValidation.sameTable(
				AggregationValidation.withColumns(dates.columns(), distinctBy));
	}
}
