package com.bakdata.conquery.io.jackson.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.dropwizard.jersey.validation.Validators;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.RecursiveComparisonAssert;

@RequiredArgsConstructor
@Accessors(chain = true, fluent = true)
@Slf4j
public class SerializationTestUtil<T> {

	private final JavaType type;
	private final Validator validator = Validators.newValidator();
	@Setter
	private CentralRegistry registry;
	@NonNull
	private Injectable[] injectables = {};

	public static <T> SerializationTestUtil<T> forType(TypeReference<T> type) {
		return new SerializationTestUtil<>(Jackson.MAPPER.getTypeFactory().constructType(type));
	}

	public static <T> SerializationTestUtil<T> forType(Class<? extends T> type) {
		return new SerializationTestUtil<>(Jackson.MAPPER.getTypeFactory().constructType(type));
	}

	public SerializationTestUtil<T> injectables(Injectable ... injectables) {
		this.injectables = injectables;
		return this;
	}

	public void test(T value, T expected) throws JSONException, IOException {
		test(
				value,
				expected,
				Jackson.MAPPER
		);
		test(
				value,
				expected,
				Jackson.BINARY_MAPPER
		);
	}

	public void test(T value) throws JSONException, IOException {
		test(value, value);
	}

	private void test(T value, T expected, ObjectMapper mapper) throws IOException {
		if (registry != null) {
			mapper = new SingletonNamespaceCollection(registry, registry).injectInto(mapper);
		}
		for (Injectable injectable : injectables) {
			mapper = injectable.injectInto(mapper);
		}
		ObjectWriter writer = mapper.writerFor(type).withView(InternalOnly.class);
		ObjectReader reader = mapper.readerFor(type).withView(InternalOnly.class);


		ValidatorHelper.failOnError(log, validator.validate(value));
		byte[] src = writer.writeValueAsBytes(value);
		T copy = reader.readValue(src);

		if (expected == null && copy == null) {
			return;
		}
		ValidatorHelper.failOnError(log, validator.validate(copy));

		//because IdentifiableImp cashes the hash
		if (value instanceof IdentifiableImpl) {
			assertThat(copy.hashCode()).isEqualTo(value.hashCode());
		}

		RecursiveComparisonAssert<?> ass = assertThat(copy)
												   .as("Unequal after copy.")
												   .usingRecursiveComparison();


		ass.isEqualTo(expected);
	}
}
