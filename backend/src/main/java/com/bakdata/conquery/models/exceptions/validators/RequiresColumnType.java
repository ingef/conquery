package com.bakdata.conquery.models.exceptions.validators;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.stores.types.MajorTypeId;
import com.bakdata.conquery.models.exceptions.validators.RequiresColumnType.RequiresColumnTypeList;
import com.bakdata.conquery.models.exceptions.validators.RequiresColumnType.RequiresMacroTypeValidator;
import org.apache.commons.lang3.ArrayUtils;


@Target({ FIELD, TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequiresMacroTypeValidator.class)
@Documented
@Repeatable(RequiresColumnTypeList.class)
public @interface RequiresColumnType {

	String message() default "The column must be one of ";

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };

	MajorTypeId[] value() default {};

	@Target({ FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface RequiresColumnTypeList {
		RequiresColumnType[] value();
	}
	
	public static class RequiresMacroTypeValidator implements ConstraintValidator<RequiresColumnType, Column> {

		private MajorTypeId[] validTypes;

		@Override
		public void initialize(RequiresColumnType anno) {
			this.validTypes=anno.value();
		}

		@Override
		public boolean isValid(Column value, ConstraintValidatorContext context) {
			context.disableDefaultConstraintViolation();
			if(value!=null && !ArrayUtils.contains(validTypes, value.getType())) {
				context
					.buildConstraintViolationWithTemplate("The column "+value.getId()+" is not one of the required types "+Arrays.toString(validTypes))
					.addConstraintViolation();
				return false;
			}
			return true;
		}
		
	}
}