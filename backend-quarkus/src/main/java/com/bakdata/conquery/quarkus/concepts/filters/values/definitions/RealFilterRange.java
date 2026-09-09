package com.bakdata.conquery.quarkus.concepts.filters.values.definitions;

import java.math.BigDecimal;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "QueryRealFilterRange", description = "Inclusive real-number range. Either boundary may be omitted.")
public record RealFilterRange(BigDecimal min, BigDecimal max) {
}
