package com.bakdata.conquery.io.jackson.serializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.NsIdResolver;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.dropwizard.jersey.validation.Validators;
import jakarta.validation.Validator;
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
	public static final Class<?>[]
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
	private NsIdResolver idResolver;
	private ObjectMapper[] objectMappers;
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
		this.objectMappers = objectMappers;
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

	public void test(T value, T expected) throws JSONException, IOException {
		if (objectMappers == null || objectMappers.length == 0) {
			fail("No objectmappers were set");
		}

		for (ObjectMapper objectMapper : objectMappers) {

			try {
				test(
						value,
						expected,
						objectMapper
				);
			}
			catch (Exception e) {
				Class<?> activeView = objectMapper.getSerializationConfig().getActiveView();
				throw new IllegalStateException("Serdes failed with object mapper using view '" + activeView + "'", e);
			}
		}
	}

	public void test(T value) throws JSONException, IOException {
		test(value, value);
	}

	private void test(T value, T expected, ObjectMapper mapper) throws IOException {

		if (idResolver != null) {
			mapper = new SingletonNamespaceCollection(idResolver).injectInto(mapper);
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

		if (forceHashCodeEqual) {
			assertThat(copy.hashCode()).isEqualTo(value.hashCode());
		}

		// Preliminary check that ids of identifiables are equal
		if (value instanceof IdentifiableImpl<?> identifiableValue) {
			assertThat(((IdentifiableImpl<?>) copy).getId()).as("the serialized value").isEqualTo(identifiableValue.getId());
		}

		RecursiveComparisonAssert<?> ass = assertThat(copy)
				.as("Unequal after copy.")
				.usingRecursiveComparison()
				.ignoringFieldsOfTypes(TYPES_TO_IGNORE);

		// Apply assertion customizations
		ass = assertCustomizer.apply(ass);

		ass.isEqualTo(expected);
	}
}
