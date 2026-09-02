package com.bakdata.conquery.sql.validation;

import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.sql.model.ResolvedQuery;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

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
