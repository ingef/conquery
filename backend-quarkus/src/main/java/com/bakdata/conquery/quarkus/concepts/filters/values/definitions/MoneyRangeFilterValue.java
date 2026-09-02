package com.bakdata.conquery.quarkus.concepts.filters.values.definitions;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "QueryMoneyRangeFilterValue", description = "A monetary range represented in the configured minor currency unit.")
@PolymorphicModelSubtype(base = FilterValue.class, id = "MONEY_RANGE")
public record MoneyRangeFilterValue(@NotNull FilterId filter, @Valid @NotNull IntegerFilterRange value) implements FilterValue {
}
