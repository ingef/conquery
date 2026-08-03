package com.bakdata.conquery.quarkus.concepts.filters.values.definitions;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.ids.FilterId;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "QueryIntegerFilterValue", description = "A single integer filter value.")
public record IntegerFilterValue(@NotNull FilterId filter, @NotNull Long value) implements FilterValue {
}
