package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.api.FilterResource.MAX_AUTOCOMPLETE_PAGE_SIZE;
import static com.bakdata.conquery.resources.api.FilterResource.MAX_AUTOCOMPLETE_TEXT_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

class FilterResourceTest {

	private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void acceptsAutocompleteRequestAtLimits() {
		final FilterResource.AutocompleteRequest request = new FilterResource.AutocompleteRequest(
			Optional.of("a".repeat(MAX_AUTOCOMPLETE_TEXT_LENGTH)),
			OptionalInt.empty(),
			OptionalInt.of(MAX_AUTOCOMPLETE_PAGE_SIZE)
		);

		assertThat(VALIDATOR.validate(request)).isEmpty();
	}

	@Test
	void rejectsAutocompleteTextExceedingLimit() {
		final FilterResource.AutocompleteRequest request = new FilterResource.AutocompleteRequest(
			Optional.of("a".repeat(MAX_AUTOCOMPLETE_TEXT_LENGTH + 1)),
			OptionalInt.empty(),
			OptionalInt.empty()
		);

		assertThat(VALIDATOR.validate(request)).singleElement()
			.extracting(
				ConstraintViolation::getPropertyPath)
			.hasToString("text");
	}

	@Test
	void rejectsAutocompletePageSizeExceedingLimit() {
		final FilterResource.AutocompleteRequest request = new FilterResource.AutocompleteRequest(
			Optional.empty(),
			OptionalInt.empty(),
			OptionalInt.of(MAX_AUTOCOMPLETE_PAGE_SIZE + 1)
		);

		assertThat(VALIDATOR.validate(request)).singleElement()
			.extracting(
				ConstraintViolation::getPropertyPath)
			.hasToString("pageSize");
	}

	@Test
	void acceptsEmptyAutocompleteOptions() {
		final FilterResource.AutocompleteRequest request = new FilterResource.AutocompleteRequest(
			Optional.empty(),
			OptionalInt.empty(),
			OptionalInt.empty()
		);

		assertThat(VALIDATOR.validate(request)).isEmpty();
	}
}
