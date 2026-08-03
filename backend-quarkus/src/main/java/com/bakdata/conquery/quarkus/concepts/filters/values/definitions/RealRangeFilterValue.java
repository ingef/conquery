package com.bakdata.conquery.quarkus.concepts.filters.values.definitions;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.ids.FilterId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "QueryRealRangeFilterValue", description = "A real-number range filter value.")
public record RealRangeFilterValue(@NotNull FilterId filter, @Valid @NotNull RealFilterRange value) implements FilterValue {
}
