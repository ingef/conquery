package com.bakdata.conquery.util.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ANNOTATION_TYPE, FIELD, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = CaffeineSpecValidator.class)
@Documented
public @interface ValidCaffeineSpec {
	String message() default "Invalid spec, see: https://github.com/ben-manes/caffeine/wiki/Specification";

	Class<?>[] groups() default {};

	@SuppressWarnings("UnusedDeclaration") Class<? extends Payload>[] payload() default {};

	/**
	 * SoftValues must be configured in the spec.
	 */
	boolean softValue() default false;
}
