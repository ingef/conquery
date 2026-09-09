package com.bakdata.conquery.quarkus.concepts.filters.values.definitions;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "QueryStringFilterValue", description = "A free-text filter value.")
@PolymorphicModelSubtype(base = FilterValue.class, id = "STRING")
public record StringFilterValue(@NotNull FilterId filter, @NotNull String value) implements FilterValue {
}
