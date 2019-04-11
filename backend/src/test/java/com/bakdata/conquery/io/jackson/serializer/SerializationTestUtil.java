package com.bakdata.conquery.io.jackson.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.validation.Validator;

import org.assertj.core.api.ObjectAssert;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializationTestUtil {

	public static <T> void testSerialization(T value, TypeReference<T> typeReference) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException {
		Validator validator = Validators.newValidator();
		ValidatorHelper.failOnError(log, validator.validate(value));
		byte[] src = Jackson.MAPPER.writeValueAsBytes(value);
		T copy = Jackson.MAPPER.readValue(src, typeReference);
		ValidatorHelper.failOnError(log, validator.validate(copy));
		assertThat(copy)
			.as("Unequal after JSON copy.")
			.isEqualToComparingFieldByFieldRecursively(value);
		copy = Jackson.BINARY_MAPPER.readValue(Jackson.BINARY_MAPPER.writeValueAsBytes(value), typeReference);
		ValidatorHelper.failOnError(log, validator.validate(copy));
		assertThat(copy)
			.as("Unequal only after BINARY copy.")
			.isEqualToComparingFieldByFieldRecursively(value);
	}
	
	public static <T> void testSerialization(T value, Class<? extends T> type, Class<?>... ignored) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException {
		Validator validator = Validators.newValidator();
		ValidatorHelper.failOnError(log, validator.validate(value));
		T copy = Jackson.MAPPER.readValue(Jackson.MAPPER.writeValueAsBytes(value), type);
		ValidatorHelper.failOnError(log, validator.validate(copy));
		ObjectAssert<T> ass = assertThat(copy)
			.as("Unequal after JSON copy.");
		for(Class<?> ig:ignored)
			ass.usingComparatorForType((a,b)->0, ig);
		ass.isEqualToComparingFieldByFieldRecursively(value);


		copy = Jackson.BINARY_MAPPER.readValue(Jackson.BINARY_MAPPER.writeValueAsBytes(value), type);
		ValidatorHelper.failOnError(log, validator.validate(copy));
		ass = assertThat(copy)
			.as("Unequal only after BINARY copy.");
		for(Class<?> ig:ignored)
			ass.usingComparatorForType((a,b)->0, ig);
		ass.isEqualToComparingFieldByFieldRecursively(value);
	}
	
	public static <T> void testSerialization(T value, Class<T> type, CentralRegistry registry) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException {
		Validator validator = Validators.newValidator();
		ValidatorHelper.failOnError(log, validator.validate(value));
		T copy = new SingletonNamespaceCollection(registry).injectInto(Jackson.MAPPER).readValue(Jackson.MAPPER.writeValueAsBytes(value), type);
		ValidatorHelper.failOnError(log, validator.validate(copy));
		assertThat(copy)
			.as("Unequal after JSON copy.")
			.isEqualToComparingFieldByFieldRecursively(value);
		copy = new SingletonNamespaceCollection(registry).injectInto(Jackson.BINARY_MAPPER).readValue(Jackson.BINARY_MAPPER.writeValueAsBytes(value), type);
		ValidatorHelper.failOnError(log, validator.validate(copy));
		assertThat(copy)
			.as("Unequal only after BINARY copy.")
			.isEqualToComparingFieldByFieldRecursively(value);
	}
}
