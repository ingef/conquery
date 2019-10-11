package com.bakdata.conquery.models.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;



@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@Constraint(validatedBy = ColumnNamerValidator.class)
public @interface ValidColumnNamer {
	String message() default "ColumnNamerScript Validation Error";

	Class<?>[] groups() default { };
	
	Class<? extends Payload>[] payload() default { };
}
