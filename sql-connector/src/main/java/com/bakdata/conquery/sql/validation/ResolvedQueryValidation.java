package com.bakdata.conquery.sql.validation;

import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import com.bakdata.conquery.sql.model.ResolvedQuery;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Validates the complete resolved query graph before SQL compilation starts. */
@UtilityClass
public final class ResolvedQueryValidation {

	public static void validate(@NonNull ResolvedQuery query, @NonNull Validator validator) {
		final Set<ConstraintViolation<ResolvedQuery>> violations = validator.validate(query);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}
