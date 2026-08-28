package com.bakdata.conquery.sql.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

final class ValidationTestSupport {

	static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

	private ValidationTestSupport() {
	}

	static <T> void assertValid(T value) {
		Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);
		assertTrue(violations.isEmpty(), () -> "Expected no constraint violations, but got: " + violations);
	}

	static <T> void assertInvalid(T value) {
		Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);
		assertFalse(violations.isEmpty(), "Expected at least one constraint violation");
	}
}
