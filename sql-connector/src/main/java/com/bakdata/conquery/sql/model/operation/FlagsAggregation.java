package com.bakdata.conquery.sql.model.operation;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.validation.AllowedColumnTypes;

public record FlagsAggregation(
		@NotEmpty Map<@NotBlank String, @NotNull @Valid @AllowedColumnTypes(MajorTypeId.BOOLEAN) ResolvedColumn> columns
) implements ResolvedAggregation {

	public FlagsAggregation {
		columns = ModelNormalization.immutableCopy(columns);
	}

	@AssertTrue(message = "flag columns must be unique")
	public boolean isUnique() {
		return columns == null || columns.values().stream().distinct().count() == columns.size();
	}

	@AssertTrue(message = "flag columns must belong to the same table")
	public boolean isOnSameTable() {
		return columns == null || AggregationValidation.sameTable(columns.values());
	}
}
