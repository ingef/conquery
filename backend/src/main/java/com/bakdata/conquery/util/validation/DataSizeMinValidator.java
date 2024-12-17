package com.bakdata.conquery.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Min;

import com.google.auto.service.AutoService;
import io.dropwizard.util.DataSize;

@AutoService(ConstraintValidator.class)
public class DataSizeMinValidator implements ConstraintValidator<Min, DataSize> {

	private long minBytes;

	@Override
	public void initialize(Min constraintAnnotation) {
		minBytes = constraintAnnotation.value();
	}

	@Override
	public boolean isValid(DataSize value, ConstraintValidatorContext context) {
		return value.toBytes() >= minBytes;
	}
}
