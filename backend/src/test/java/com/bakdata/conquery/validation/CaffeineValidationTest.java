package com.bakdata.conquery.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import com.bakdata.conquery.util.validation.ValidCaffeineSpec;
import org.junit.jupiter.api.Test;

public class CaffeineValidationTest {

	private final static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void correctSpec() {
		Set<ConstraintViolation<Container>> softValues = validator.validate(new Container("softValues,maximumSize=1"));

		assertThat(softValues).hasSize(0);

	}

	@Test
	void correctSoftSpec() {
		Set<ConstraintViolation<ContainerSoft>> softValues = validator.validate(new ContainerSoft("softValues,maximumSize=1"));

		assertThat(softValues).hasSize(0);

	}

	@Test
	void missingSoftValuesSpec() {
		Set<ConstraintViolation<ContainerSoft>> softValues = validator.validate(new ContainerSoft("maximumSize=1"));

		assertThat(softValues).hasSize(2);
	}

	@Test
	void unparsableSpec() {
		Set<ConstraintViolation<ContainerSoft>> softValues = validator.validate(new ContainerSoft("maximumSize=1=2,softValues"));

		assertThat(softValues).hasSize(2);
	}


	@Test
	void unparsableAndMissingSoftValuesSpec() {
		Set<ConstraintViolation<ContainerSoft>> softValues = validator.validate(new ContainerSoft("maximumSize=1=2"));

		assertThat(softValues).hasSize(3);
	}

	record ContainerSoft(@ValidCaffeineSpec(softValue = true) String spec) {}

	record Container(@ValidCaffeineSpec() String spec) {}



}
