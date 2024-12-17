package com.bakdata.conquery.util.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.util.DataSize;
import lombok.Data;
import org.junit.jupiter.api.Test;

public class DataSizeValidationTest {

	Validator VALIDATOR = Validators.newValidator();


	@Test
	void inBounds() {
		Container container = new Container(DataSize.bytes(5));

		Set<ConstraintViolation<Container>> validate = VALIDATOR.validate(container);

		assertThat(validate).isEmpty();

	}

	@Test
	void onMaxBound() {
		Container container = new Container(DataSize.bytes(6));

		Set<ConstraintViolation<Container>> validate = VALIDATOR.validate(container);

		assertThat(validate).isEmpty();

	}

	@Test
	void onMinBound() {
		Container container = new Container(DataSize.bytes(3));

		Set<ConstraintViolation<Container>> validate = VALIDATOR.validate(container);

		assertThat(validate).isEmpty();

	}

	@Test
	void maxedOut() {
		Container container = new Container(DataSize.bytes(7));

		Set<ConstraintViolation<Container>> validate = VALIDATOR.validate(container);

		assertThat(validate).hasSize(1);

	}

	@Test
	void minedOut() {
		Container container = new Container(DataSize.bytes(2));

		Set<ConstraintViolation<Container>> validate = VALIDATOR.validate(container);

		assertThat(validate).hasSize(1);

	}

	@Data
	private static class Container {
		@Min(3)
		@Max(6)
		private final DataSize size;
	}
}
