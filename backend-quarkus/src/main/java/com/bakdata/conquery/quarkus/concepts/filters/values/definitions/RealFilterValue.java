package com.bakdata.conquery.quarkus.concepts.filters.values.definitions;

import java.math.BigDecimal;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "QueryRealFilterValue", description = "A single real-number filter value.")
@PolymorphicModelSubtype(base = FilterValue.class, id = "REAL")
public record RealFilterValue(@NotNull FilterId filter, @NotNull BigDecimal value) implements FilterValue {
}
