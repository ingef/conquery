package com.bakdata.conquery.sql.model.operation.aggregation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.validation.AllowedColumnTypes;

/** Maximum number of distinct quarters containing events in any calendar year. */
public record QuartersInYearAggregation(
		@NotNull @Valid @AllowedColumnTypes(MajorTypeId.DATE) ResolvedColumn column
) implements ResolvedAggregation {
}
