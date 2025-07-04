package com.bakdata.conquery.io.jackson.serializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import jakarta.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.dropwizard.jersey.validation.Validators;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
	public static final Class<?>[]
			TYPES_TO_IGNORE =
			new Class[]{
					AtomicInteger.class,
					Double.class,
					SoftReference.class,
					ThreadLocal.class,
					User.ShiroUserAdapter.class,
					Validator.class,
					WeakReference.class,
					CompletableFuture.class,
					NamespacedStorageProvider.class
			};

	private final JavaType type;
	private final Validator validator = Validators.newValidator();
	private List<ObjectMapper> objectMappers = Collections.emptyList();
	@NonNull
	private Injectable[] injectables = {};

	private boolean forceHashCodeEqual = false;

	private UnaryOperator<RecursiveComparisonAssert<?>> assertCustomizer = UnaryOperator.identity();

	public static <T> SerializationTestUtil<T> forType(TypeReference<T> type) {
		return new SerializationTestUtil<>(Jackson.MAPPER.getTypeFactory().constructType(type));
	}

	public static <T> SerializationTestUtil<T> forType(Class<? extends T> type) {
		return new SerializationTestUtil<>(Jackson.MAPPER.copy().getTypeFactory().constructType(type));
	}

	public static <T> SerializationTestUtil<T[]> forArrayType(TypeReference<T> elementType) {
		return new SerializationTestUtil<>(Jackson.MAPPER.getTypeFactory().constructArrayType(Jackson.MAPPER.getTypeFactory().constructType(elementType)));
	}

	public SerializationTestUtil<T> objectMappers(ObjectMapper... objectMappers) {
		this.objectMappers = Arrays.asList(objectMappers);
		return this;
	}

	public SerializationTestUtil<T> injectables(Injectable... injectables) {
		this.injectables = injectables;
		return this;
	}

	public SerializationTestUtil<T> checkHashCode() {
		this.forceHashCodeEqual = true;
		return this;
	}

	public SerializationTestUtil<T> customizingAssertion(UnaryOperator<RecursiveComparisonAssert<?>> assertCustomizer) {
		this.assertCustomizer = assertCustomizer;
		return this;
	}

	public void test(T value) throws JSONException, IOException {
		test(value, value);
	}

	public void test(T value, T expected) throws JSONException, IOException {
		if (objectMappers.isEmpty()) {
			fail("No ObjectMappers were provided.");
		}

		for (ObjectMapper objectMapper : objectMappers) {
			try {
				test(
						value,
						expected,
						objectMapper
				);
			} catch (Exception|Error e) {
				Class<?> activeView = objectMapper.getSerializationConfig().getActiveView();
				throw new IllegalStateException("Serdes failed with object mapper using view '" + activeView + "'", e);
			}
		}
	}

	private void test(T value, T expected, ObjectMapper mapper) throws IOException {

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

		if (forceHashCodeEqual) {
			assertThat(copy.hashCode()).isEqualTo(value.hashCode());
		}

		// Preliminary check that ids of identifiables are equal
		if (value instanceof Identifiable<?, ?> identifiableValue) {
			assertThat(((Identifiable<?, ?>) copy).getId())
					.as("the serialized value")
					.isEqualTo(identifiableValue.getId());
		}

		RecursiveComparisonAssert<?> ass = assertThat(copy)
				.as("Unequal after copy.")
				.usingRecursiveComparison()
				.usingOverriddenEquals()
				.ignoringFieldsOfTypes(TYPES_TO_IGNORE)
				.ignoringFields("metaStorage", "namespacedStorageProvider");

		// Apply assertion customizations
		ass = assertCustomizer.apply(ass);

		ass.isEqualTo(expected);
	}
}
