package com.bakdata.conquery.models.exceptions.validators;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.EnumSet;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.bakdata.conquery.models.events.stores.types.MajorTypeId;
import com.bakdata.conquery.models.events.stores.types.MajorTypeIdHolder;
import com.bakdata.conquery.models.exceptions.validators.EqualsNotColumnType.EqualsNotColumnTypeList;
import com.bakdata.conquery.models.exceptions.validators.EqualsNotColumnType.EqualsNotColumnTypeValidator;


@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EqualsNotColumnTypeValidator.class)
@Documented
@Repeatable(EqualsNotColumnTypeList.class)
public @interface EqualsNotColumnType {
	
	String message() default "The selected type '${validatedValue}' may not equal any of {value}";

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };
	
	MajorTypeId[] value();

	@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface EqualsNotColumnTypeList {
		EqualsNotColumnType[] value();
	}
	
	public static class EqualsNotColumnTypeValidator implements ConstraintValidator<EqualsNotColumnType, MajorTypeIdHolder> {

		private EnumSet<MajorTypeId> forbidden;

		@Override
		public void initialize(EqualsNotColumnType anno) {
			this.forbidden=EnumSet.copyOf(Arrays.asList(anno.value()));
		}
		
		@Override
		public boolean isValid(MajorTypeIdHolder value, ConstraintValidatorContext context) {
			if(value==null) {
				context.disableDefaultConstraintViolation();
				context
					.buildConstraintViolationWithTemplate("The resulting type is null")
					.addConstraintViolation();
				return false;
			}

			if(forbidden.contains(value.getTypeId())) {
				return false;
			}
			
			return true;
		}
	}
}