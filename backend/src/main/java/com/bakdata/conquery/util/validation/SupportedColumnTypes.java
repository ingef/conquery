package com.bakdata.conquery.util.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import com.bakdata.conquery.models.events.MajorTypeId;

@Target({ANNOTATION_TYPE, FIELD, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = SupportedColumnTypesValidator.class)
@Documented
public @interface SupportedColumnTypes {

	String message() default "Column type is not supported";

	Class<?>[] groups() default {};

	@SuppressWarnings("UnusedDeclaration") Class<? extends Payload>[] payload() default {};

	MajorTypeId[] value() default {};

	/**
	 * Shortcut for adding types of {@link MajorTypeId#NUMERIC}.
	 */
	boolean numericTypes() default false;

}
