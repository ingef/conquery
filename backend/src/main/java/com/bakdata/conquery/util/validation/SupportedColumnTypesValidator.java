package com.bakdata.conquery.util.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;

public class SupportedColumnTypesValidator implements ConstraintValidator<SupportedColumnTypes, ColumnId> {
	private final Set<MajorTypeId> supportedTypes = new HashSet<>();

	@Override
	public void initialize(SupportedColumnTypes constraintAnnotation) {
		this.supportedTypes.addAll(Arrays.asList(constraintAnnotation.value()));
		if (constraintAnnotation.numericTypes()) {
			this.supportedTypes.addAll(MajorTypeId.NUMERIC);
		}
	}

	@Override
	public boolean isValid(ColumnId value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}

		if (supportedTypes.isEmpty()) {
			return true;
		}

		final Column resolved = value.get();
		if (resolved == null) {
			return true;
		}

		MajorTypeId type = resolved.getType();
		boolean contains = supportedTypes.contains(type);
		if (contains) {
			return true;
		}

		context.buildConstraintViolationWithTemplate(
				"Column type '%s' is not supported. Supported types are: %s".formatted( type, supportedTypes)
		).addConstraintViolation();

		return false;
	}
}
