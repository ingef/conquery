package com.bakdata.conquery.sql.query.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bakdata.conquery.models.datasets.ColumnType;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = AllowedColumnTypesValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedColumnTypes {

	String message() default "column type is not supported";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	ColumnType[] value();
}
