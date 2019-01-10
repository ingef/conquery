package com.bakdata.conquery.models.exceptions.validators;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.*;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.bakdata.conquery.models.exceptions.validators.ValidName.ValidNameList;
import com.bakdata.conquery.models.exceptions.validators.ValidName.ValidNameValidator;
import com.bakdata.conquery.models.identifiable.Named;

import lombok.extern.slf4j.Slf4j;


@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidNameValidator.class)
@Documented
@Repeatable(ValidNameList.class)
public @interface ValidName {
	
	String message() default "The name is not valid";

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };

	@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface ValidNameList {
		ValidName[] value();
	}
	
	@Slf4j
	public static class ValidNameValidator implements ConstraintValidator<ValidName, String> {

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			context.disableDefaultConstraintViolation();
			if(value==null) {
				context
					.buildConstraintViolationWithTemplate("The name is null")
					.addConstraintViolation();
				return false;
			}
			else {
				if(value.length()==0) {
					context
						.buildConstraintViolationWithTemplate("A name can not be an empty string")
						.addConstraintViolation();
					return false;
				}
				if(!Named.VALID_CHARACTERS.matchesAllOf(value)) {
					context
						.buildConstraintViolationWithTemplate("The name '"+value+"' is not a valid name because it contains the characters '"+Named.VALID_CHARACTERS.removeFrom(value)+"'")
						.addConstraintViolation();
					return false;
				}
			}
			return true;
		}

		@Override
		public void initialize(ValidName constraintAnnotation) {}
	}
}