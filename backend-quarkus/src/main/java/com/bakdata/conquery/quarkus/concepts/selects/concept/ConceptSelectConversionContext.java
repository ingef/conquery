package com.bakdata.conquery.quarkus.concepts.selects.concept;

import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.ConceptSelectId;
import com.bakdata.conquery.quarkus.ids.IdPartSanitizer;

public record ConceptSelectConversionContext(
		ConceptId conceptId,
		FallbackReporter fallbackReporter
) {

	public ConceptSelectId selectId(String name) {
		return new ConceptSelectId(conceptId, name);
	}

	public String idPartFromPreferredOrFallback(String preferred, String fallback, String type) {
		if (preferred != null && !preferred.isBlank()) {
			return preferred.trim();
		}
		String source = Optional.ofNullable(fallback).filter(value -> !value.isBlank())
				.orElseThrow(() -> new IllegalArgumentException("Concept select " + type + " must define name or label."));
		String sanitized = IdPartSanitizer.sanitize(source, "concept select id fallback");
		fallbackReporter.record("concept select id", type, source, sanitized);
		return sanitized;
	}

	@FunctionalInterface
	public interface FallbackReporter {
		void record(String idType, Object context, String fallbackValue, String sanitized);
	}
}
