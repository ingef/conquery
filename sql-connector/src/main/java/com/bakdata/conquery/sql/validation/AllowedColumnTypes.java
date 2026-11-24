package com.bakdata.conquery.sql.validation;

import java.lang.annotation.*;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import com.bakdata.conquery.models.datasets.MajorTypeId;

@Documented
@Constraint(validatedBy = AllowedColumnTypesValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedColumnTypes {

	String message() default "column type is not supported";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	MajorTypeId[] value();
}
