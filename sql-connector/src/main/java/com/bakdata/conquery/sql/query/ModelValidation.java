package com.bakdata.conquery.sql.query;

import java.util.Collection;
import java.util.Objects;

final class ModelValidation {

	private ModelValidation() {
	}

	static String requireNonBlank(String value, String name) {
		Objects.requireNonNull(value, name);
		if (value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
		return value;
	}

	static <T> Collection<T> requireNotEmpty(Collection<T> values, String name) {
		Objects.requireNonNull(values, name);
		if (values.isEmpty()) {
			throw new IllegalArgumentException(name + " must not be empty");
		}
		return values;
	}
}
