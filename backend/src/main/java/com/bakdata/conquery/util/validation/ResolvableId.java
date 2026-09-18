package com.bakdata.conquery.util.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ANNOTATION_TYPE, FIELD, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = ResolvableIdValidator.class)
@Documented
public @interface ResolvableId {
	String message() default "Id is unknown";

	Class<?>[] groups() default {};

	@SuppressWarnings("UnusedDeclaration") Class<? extends Payload>[] payload() default {};
}
