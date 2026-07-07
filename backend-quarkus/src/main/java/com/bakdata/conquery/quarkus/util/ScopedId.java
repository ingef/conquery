package com.bakdata.conquery.quarkus.util;

import java.util.Optional;

public final class ScopedId {

	private ScopedId() {
	}

	public static Optional<String> extractDatasetId(String scopedId) {
		if (scopedId == null || scopedId.isBlank()) {
			return Optional.empty();
		}
		int separator = scopedId.indexOf('.');
		if (separator <= 0) {
			return Optional.of(scopedId);
		}
		return Optional.of(scopedId.substring(0, separator));
	}

}
