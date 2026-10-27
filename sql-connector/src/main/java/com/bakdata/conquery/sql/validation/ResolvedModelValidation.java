package com.bakdata.conquery.sql.validation;

import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/** Validates a complete resolved model graph before SQL translation starts. */
@RequiredArgsConstructor
public final class ResolvedModelValidation {

	@NonNull
	private final Validator validator;

	public <T> void validate(@NonNull T model) {
		Set<ConstraintViolation<T>> violations = validator.validate(model);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}
