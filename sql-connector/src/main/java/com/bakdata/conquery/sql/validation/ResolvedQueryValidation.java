package com.bakdata.conquery.sql.validation;

import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import com.bakdata.conquery.sql.model.ResolvedQuery;
import lombok.NonNull;

/** Validates the complete resolved query graph before SQL compilation starts. */
public final class ResolvedQueryValidation {

	private final Validator validator;

	public ResolvedQueryValidation(@NonNull Validator validator) {
		this.validator = validator;
	}

	public void validate(@NonNull ResolvedQuery query) {
		Set<ConstraintViolation<ResolvedQuery>> violations = validator.validate(query);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}
