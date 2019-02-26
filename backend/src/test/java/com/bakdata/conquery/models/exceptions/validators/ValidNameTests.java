package com.bakdata.conquery.models.exceptions.validators;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.AId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public class ValidNameTests {
	
	private static Validator VALIDATOR;
	
	//{index}[name={0}, valid={1}]
	@TestFactory
	public static List<Arguments> data() {
		return Arrays.asList(
			Arguments.of("foo_baaar_fooo",	true),
			Arguments.of("hans im glück",	false),
			Arguments.of("P10K",			true),
			Arguments.of("test",			true),
			Arguments.of("_asdassd",		true),
			Arguments.of("1asd_asdasd",		true)
		);
	}

	@BeforeAll
	public static void setUp() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		VALIDATOR = factory.getValidator();
	}
	
	@ParameterizedTest @MethodSource("data")
	public void validity(String name, boolean expectValid) {
		
		Name wrapper = new Name();
		wrapper.setName(name);
		
		if(VALIDATOR.validate(wrapper).isEmpty()) {
			if(!expectValid) {
				fail("'"+name+"' was not validated as valid");
			}
		}
		else {
			if(expectValid) {
				fail("'"+name+"' was validated as valid");
			}
		}
	}
	
	static class Name extends NamedImpl<NameId> {
		@Override
		public NameId createId() {
			return new NameId(getName());
		}
	}
	
	@Getter @EqualsAndHashCode @AllArgsConstructor
	static class NameId extends AId<Name> {

		private String name;

		@Override
		public void collectComponents(List<Object> components) {
			components.add(name);
		}
		
	}
}