package com.bakdata.conquery.io.jackson.serializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.entities.User;
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

	/**
	 * These don't seem to behave well in combination with recursiveComparison.
	 */
	private static final Class<?>[]
			TYPES_TO_IGNORE =
			new Class[]{
					AtomicInteger.class,
					Double.class,
					SoftReference.class,
					ThreadLocal.class,
					User.ShiroUserAdapter.class,
					Validator.class,
					WeakReference.class
			};

	private final JavaType type;
	private final Validator validator = Validators.newValidator();
	@Setter
	private CentralRegistry registry;
	private ObjectMapper[] objectMappers;
	@NonNull
	private Injectable[] injectables = {};

	public static <T> SerializationTestUtil<T> forType(TypeReference<T> type) {
		return new SerializationTestUtil<>(Jackson.MAPPER.getTypeFactory().constructType(type));
	}

	public static <T> SerializationTestUtil<T> forType(Class<? extends T> type) {
		return new SerializationTestUtil<>(Jackson.MAPPER.copy().getTypeFactory().constructType(type));
	}

	public SerializationTestUtil<T> objectMappers(ObjectMapper... objectMappers) {
		this.objectMappers = objectMappers;
		return this;
	}

	public SerializationTestUtil<T> injectables(Injectable... injectables) {
		this.injectables = injectables;
		return this;
	}

	public void test(T value, T expected) throws JSONException, IOException {
		if (objectMappers == null || objectMappers.length == 0) {
			fail("No objectmappers were set");
		}

		for (ObjectMapper objectMapper : objectMappers) {
			test(
					value,
					expected,
					objectMapper
			);
		}
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
		ObjectWriter writer = mapper.writerFor(type);
		ObjectReader reader = mapper.readerFor(type);


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
				.usingRecursiveComparison().ignoringFieldsOfTypes(TYPES_TO_IGNORE);


		ass.isEqualTo(expected);
	}
}
