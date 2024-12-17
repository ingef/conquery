package com.bakdata.conquery.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Max;

import com.google.auto.service.AutoService;
import io.dropwizard.util.DataSize;

@AutoService(ConstraintValidator.class)
public class DataSizeMaxValidator implements ConstraintValidator<Max, DataSize> {

	private long maxBytes;

	@Override
	public void initialize(Max constraintAnnotation) {
		maxBytes = constraintAnnotation.value();
	}

	@Override
	public boolean isValid(DataSize value, ConstraintValidatorContext context) {
		return value.toBytes() <= maxBytes;
	}
}
