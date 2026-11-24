package com.bakdata.conquery.sql.model.operation.aggregation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;
import com.bakdata.conquery.sql.model.schema.DateColumns;

public record CountQuartersAggregation(@NotNull @Valid DateColumns dates) implements ResolvedAggregation {
}
