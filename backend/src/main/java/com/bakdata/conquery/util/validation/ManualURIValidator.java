package com.bakdata.conquery.util.validation;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ManualURIValidator implements ConstraintValidator<ManualURI, URI> {

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
