package com.bakdata.conquery.models.types;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.bakdata.conquery.models.config.ValidColumnNamer;

public class ColumnNamerTest {
	
	private static Validator validator;

	@BeforeAll
	public static void setUpValidator() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}
	
	// TEST null script

	private static class NullScript{
		@ValidColumnNamer
		private String SCRIPT_NULL = null;
	}
    
    @Test
    void scriptIsNull () {
    	assertThat(validator.validate(new NullScript())).isNotEmpty();
    }
    
    // TEST empty script

	private static class EmptyScript{
		@ValidColumnNamer
		private String SCRIPT_NULL = "";
	}
    
    @Test
    void scriptIsEmpty () {
    	assertThat(validator.validate(new EmptyScript())).isNotEmpty();
    }
    
    // TEST valid script

	private static class ValidScript{
		@ValidColumnNamer
		private String SCRIPT_NULL = "java.lang.String.format(\"%s %s\",columnInfo.getCqConcept().getLabel(),columnInfo.getSelect().getLabel())";
	}
    
    @Test
    void scriptIsValid () {
    	assertThat(validator.validate(new ValidScript())).isEmpty();
    }
}
