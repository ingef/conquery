package com.bakdata.conquery.util.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validate Shiro permissions
 */
@NotBlank
@Pattern(
	regexp = ValidConqueryPermission.PERMISSION_PATTERN,
	message = "Provide a valid shiro permission string with 3 parts at most, following this pattern: " + ValidConqueryPermission.PERMISSION_PATTERN)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {}) // No custom validator needed if only using built-ins
public @interface ValidConqueryPermission {
	String PERMISSION_PATTERN = "^[\\w,*-]+(?::[\\w,*@-]+){0,2}$";

	String message() default "Invalid permission";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
