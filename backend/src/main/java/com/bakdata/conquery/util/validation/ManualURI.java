package com.bakdata.conquery.util.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ANNOTATION_TYPE, FIELD, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = ManualURIValidator.class)
@Documented
public @interface ManualURI {
	String message() default "";

	Class<?>[] groups() default {};

	@SuppressWarnings("UnusedDeclaration") Class<? extends Payload>[] payload() default {};
}
