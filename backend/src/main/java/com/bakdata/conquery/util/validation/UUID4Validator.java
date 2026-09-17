package com.bakdata.conquery.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.UUID;

public class UUID4Validator implements ConstraintValidator<ValidUUID4 , UUID> {

	@Override
	public boolean isValid(UUID id, ConstraintValidatorContext context) {
		return id == null || id.version() == 4;
	}
}
