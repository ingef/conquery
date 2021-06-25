package com.bakdata.conquery.models.exceptions.validators;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.bakdata.conquery.models.exceptions.validators.ValidCSVFormat.ValidCSVFormatList;
import com.bakdata.conquery.models.exceptions.validators.ValidCSVFormat.ValidCSVFormatValidator;
import com.bakdata.conquery.apiv1.query.concept.specific.CQExternal.FormatColumn;


@Target({ FIELD, TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCSVFormatValidator.class)
@Documented
@Repeatable(ValidCSVFormatList.class)
public @interface ValidCSVFormat {

	String message() default "no valid configuration";

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };

	@Target({ FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface ValidCSVFormatList {
		ValidCSVFormat[] value();
	}
	
	public static class ValidCSVFormatValidator implements ConstraintValidator<ValidCSVFormat, List<FormatColumn>> {

		@Override
		public void initialize(ValidCSVFormat anno) {}

		@Override
		public boolean isValid(List<FormatColumn> value, ConstraintValidatorContext context) {
			context.disableDefaultConstraintViolation();
			if(value!=null) {
				boolean correct = true;
				if(value.stream().anyMatch(Objects::isNull)) {
					context
						.buildConstraintViolationWithTemplate("Null is not allowed as part of the format")
						.addConstraintViolation();
					correct = false;
				}
				for(FormatColumn fc : FormatColumn.values()) {
					if(!fc.isDuplicatesAllowed() && value.stream().filter(v->fc.equals(v)).count() > 1) {
						context
							.buildConstraintViolationWithTemplate("The format column "+fc+" is may not appear more than once")
							.addConstraintViolation();
						correct = false;
					}
				}
				if(!value.contains(FormatColumn.ID)) {
					context
						.buildConstraintViolationWithTemplate("The format does not contain "+FormatColumn.ID)
						.addConstraintViolation();
					correct = false;
				}
				long dateTypes =
					value.stream().map(FormatColumn::getDateFormat).filter(Objects::nonNull).distinct().count();
				if(dateTypes > 1) {
					context
						.buildConstraintViolationWithTemplate("The format may not contain more than one of DATE_RANGE, EVENT_DATE and (START_DATE and/or END_DATE)")
						.addConstraintViolation();
					correct = false;
				}
				
				
				return correct;
			}
			return true;
		}
		
	}
}