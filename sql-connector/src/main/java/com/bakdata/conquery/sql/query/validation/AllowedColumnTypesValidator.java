package com.bakdata.conquery.sql.query.validation;

import java.util.EnumSet;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.query.schema.ResolvedColumn;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class AllowedColumnTypesValidator implements ConstraintValidator<AllowedColumnTypes, ResolvedColumn> {

	private EnumSet<ColumnType> allowedTypes;

	@Override
	public void initialize(AllowedColumnTypes constraint) {
		allowedTypes = EnumSet.copyOf(java.util.List.of(constraint.value()));
	}

	@Override
	public boolean isValid(ResolvedColumn column, ConstraintValidatorContext context) {
		return column == null || column.type() == null || allowedTypes.contains(column.type());
	}
}
