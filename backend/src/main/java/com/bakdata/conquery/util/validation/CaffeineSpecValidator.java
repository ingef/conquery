package com.bakdata.conquery.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.github.benmanes.caffeine.cache.CaffeineSpec;

public class CaffeineSpecValidator implements ConstraintValidator<ValidCaffeineSpec, String> {

	private boolean softValue;

	@Override
	public void initialize(ValidCaffeineSpec constraintAnnotation) {
		softValue = constraintAnnotation.softValue();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		boolean isValid = true;
		try {
			CaffeineSpec.parse(value);
		}
		catch (Exception e) {
			isValid = false;
			context.buildConstraintViolationWithTemplate("CaffeineSpec is invalid:" + e.getMessage()).addConstraintViolation();
		}

		if (softValue) {
			// As long as we work with concrete objects (not Proxies) we need to ensure that we always hand out the one already referenced value if possible
			if(!value.contains("softValues")) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("Currently softValues need to be configured").addConstraintViolation();
			}
		}

		return isValid;
	}
}
