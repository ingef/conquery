package com.bakdata.conquery.sql.model.operation.aggregation;

import java.util.List;
import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.validation.AllowedColumnTypes;

public record SumAggregation(
		@NotNull @Valid @AllowedColumnTypes({MajorTypeId.INTEGER, MajorTypeId.REAL, MajorTypeId.DECIMAL, MajorTypeId.MONEY})
		ResolvedColumn column,
		@NotNull Optional<@Valid @AllowedColumnTypes({
				MajorTypeId.INTEGER, MajorTypeId.REAL, MajorTypeId.DECIMAL, MajorTypeId.MONEY
		}) ResolvedColumn> subtractColumn,
		@NotNull List<@NotNull @Valid ResolvedColumn> distinctBy
) implements ResolvedAggregation {

	public SumAggregation {
		distinctBy = ModelNormalization.immutableCopy(distinctBy);
	}

	@AssertTrue(message = "column and subtractColumn must have the same type")
	public boolean isMatchingColumnType() {
		return column == null || subtractColumn == null || subtractColumn.isEmpty()
				|| column.type() == subtractColumn.get().type();
	}

	@AssertTrue(message = "sum columns must belong to the same table")
	public boolean isOnSameTable() {
		if (subtractColumn == null) {
			return true;
		}
		final List<ResolvedColumn> columns = AggregationValidation.withColumn(column, distinctBy);
		subtractColumn.ifPresent(columns::add);
		return AggregationValidation.sameTable(columns);
	}
}
