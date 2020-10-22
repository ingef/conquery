package com.bakdata.conquery.models.exceptions.validators;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.bakdata.conquery.models.exceptions.validators.ExistingFile.ExistingFileList;
import com.bakdata.conquery.models.exceptions.validators.ExistingFile.ExistingFileValidator;
import lombok.extern.slf4j.Slf4j;

@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExistingFileValidator.class)
@Documented
@Repeatable(ExistingFileList.class)
public @interface ExistingFile {

	String message() default "The file does not exist";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	boolean directory() default false;

	@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface ExistingFileList {
		ExistingFile[] value();
	}

	@Slf4j
	class ExistingFileValidator implements ConstraintValidator<ExistingFile, File> {

		private boolean directory;

		@Override
		public void initialize(ExistingFile anno) {
			this.directory = anno.directory();
		}

		@Override
		public boolean isValid(File value, ConstraintValidatorContext context) {
			context.disableDefaultConstraintViolation();
			if (value == null) {
				context
						.buildConstraintViolationWithTemplate("The File/Directory is null")
						.addConstraintViolation();
				return false;
			}
			try {
				if (directory && !value.isDirectory()) {
					context
							.buildConstraintViolationWithTemplate("The Directory " + value.getAbsoluteFile() + " does not exist or is not a directory")
							.addConstraintViolation();
					return false;
				} else if (!directory && !value.isFile()) {
					context
							.buildConstraintViolationWithTemplate("The File " + value.getAbsoluteFile() + " does not exist or is not a file")
							.addConstraintViolation();
					return false;
				} else {
					return true;
				}
			} catch (Exception e) {
				log.error("Failed to construct the canonical path of " + value.getAbsolutePath(), e);
				context
						.buildConstraintViolationWithTemplate("Failed to construct the canonical path of " + value.getAbsolutePath())
						.addConstraintViolation();
				return false;
			}
		}
	}
}