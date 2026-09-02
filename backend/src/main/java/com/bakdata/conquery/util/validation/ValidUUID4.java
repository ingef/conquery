package com.bakdata.conquery.util.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ANNOTATION_TYPE, FIELD, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = UUID4Validator.class)
@Documented
public @interface ValidUUID4 {
	String message() default "UUID is not version 4";

	Class<?>[] groups() default {};

	@SuppressWarnings("UnusedDeclaration") Class<? extends Payload>[] payload() default {};
}