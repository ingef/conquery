package com.bakdata.conquery.sql.model.operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.schema.DateColumns;

public record CountQuartersAggregation(@NotNull @Valid DateColumns dates) implements ResolvedAggregation {
}
