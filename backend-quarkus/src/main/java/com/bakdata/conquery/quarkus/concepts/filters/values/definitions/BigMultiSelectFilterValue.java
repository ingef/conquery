package com.bakdata.conquery.quarkus.concepts.filters.values.definitions;

import java.util.Set;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "QueryBigMultiSelectFilterValue", description = "A potentially large set of selected filter values.")
@PolymorphicModelSubtype(base = FilterValue.class, id = "BIG_MULTI_SELECT")
public record BigMultiSelectFilterValue(@NotNull FilterId filter, @NotNull @NotEmpty Set<@NotNull String> value) implements FilterValue {
}
