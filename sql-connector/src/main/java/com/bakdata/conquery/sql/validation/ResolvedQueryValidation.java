package com.bakdata.conquery.sql.validation;

import java.util.Objects;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import com.bakdata.conquery.sql.model.ResolvedQuery;

/** Validates the complete resolved query graph before SQL compilation starts. */
public final class ResolvedQueryValidation {

	private final Validator validator;

	public ResolvedQueryValidation(Validator validator) {
		this.validator = Objects.requireNonNull(validator, "validator");
	}

	public void validate(ResolvedQuery query) {
		Set<ConstraintViolation<ResolvedQuery>> violations = validator.validate(Objects.requireNonNull(query, "query"));
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}
