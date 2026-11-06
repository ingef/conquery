package com.bakdata.conquery.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.bakdata.conquery.models.identifiable.ids.Id;

public class ResolvableIdValidator implements ConstraintValidator<ResolvableId, Id<?,?>> {
	@Override
	public boolean isValid(Id<?,?> id, ConstraintValidatorContext constraintValidatorContext) {
		if (id == null) {
			// NotNull validation should cover this
			return true;
		}

		Object resolved = id.get();

		if (resolved == null) {
			constraintValidatorContext.buildConstraintViolationWithTemplate("The id '%s' of type %s cannot be resolved".formatted(id, id.getClass())).addConstraintViolation();
			return false;
		}

		return true;
	}
}
