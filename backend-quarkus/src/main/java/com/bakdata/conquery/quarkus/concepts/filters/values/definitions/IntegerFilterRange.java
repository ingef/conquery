package com.bakdata.conquery.quarkus.concepts.filters.values.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "QueryIntegerFilterRange", description = "Inclusive integer range. Either boundary may be omitted.")
public record IntegerFilterRange(Long min, Long max) {
}
