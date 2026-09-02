package com.bakdata.conquery.quarkus.concepts.filters.values.definitions;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "QueryIntegerRangeFilterValue", description = "An integer range filter value.")
@PolymorphicModelSubtype(base = FilterValue.class, id = "INTEGER_RANGE")
public record IntegerRangeFilterValue(@NotNull FilterId filter, @Valid @NotNull IntegerFilterRange value) implements FilterValue {
}
