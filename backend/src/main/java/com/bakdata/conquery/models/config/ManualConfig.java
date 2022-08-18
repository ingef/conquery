package com.bakdata.conquery.models.config;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.bakdata.conquery.io.cps.CPSType;
import lombok.Getter;
import lombok.Setter;

@CPSType(id = "MANUAL", base = PluginConfig.class)
@Getter
@Setter
public class ManualConfig implements PluginConfig {


	private Map<String, @ManualURI URI> forms = Collections.emptyMap();

	@Target({ANNOTATION_TYPE, FIELD, TYPE_USE})
	@Retention(RUNTIME)
	@Constraint(validatedBy = ManualURIValidator.class)
	@Documented
	public @interface ManualURI {
		String message() default "";

		Class<?>[] groups() default {};

		@SuppressWarnings("UnusedDeclaration") Class<? extends Payload>[] payload() default {};
	}

	public static class ManualURIValidator implements ConstraintValidator<ManualURI, URI> {

		@Override
		public boolean isValid(URI value, ConstraintValidatorContext context) {

			if (value == null) {
				return true;
			}

			context.disableDefaultConstraintViolation();

			boolean isValid = true;

			if (value.isOpaque()) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("The URI was opaque")
					   .addConstraintViolation();
			}

			if (value.isAbsolute()) {
				try {
					var unused = value.toURL();
				}
				catch (MalformedURLException e) {
					isValid = false;
					context.buildConstraintViolationWithTemplate("Absolute URI is not a valid URL (" + e.getMessage() + ")")
						   .addConstraintViolation();
				}
				if (!List.of("http", "https").contains(value.getScheme())) {
					isValid = false;
					context.buildConstraintViolationWithTemplate("The URI has an unsupported Scheme: " + value.getScheme())
						   .addConstraintViolation();
				}
			}
			else {
				if (value.getAuthority() != null) {
					isValid = false;
					context.buildConstraintViolationWithTemplate("The URI is not absolute but has an authority: " + value.getAuthority())
						   .addConstraintViolation();
				}
			}

			return isValid;
		}
	}
}
