package com.bakdata.conquery.quarkus.concepts.filters;

@FunctionalInterface
public interface FilterFallbackReporter {
	void record(String idType, Object context, String fallbackValue, String sanitized);
}
