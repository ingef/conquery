package com.bakdata.conquery.models.identifiable.ids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.IdUtil.Parser;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.TestNamespacedStorageProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.dropwizard.jersey.validation.Validators;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IdTests {

	public static final NamespacedStorageProvider STORAGE = new TestNamespacedStorageProvider();

	public static Stream<Arguments> reflectionTest() {
		return CPSTypeIdResolver
				.SCAN_RESULT
				.getClassesImplementing(Identifiable.class.getName()).loadClasses()
				.stream()
				.filter(cl -> !cl.isInterface())
				.filter(cl -> !Modifier.isAbstract(cl.getModifiers()))
				//filter test classes
				.filter(cl -> !cl.toString().toLowerCase().contains("test"))
				.map(cl -> {

					Class<?> idClazz = null;
					// Try to get the specific Id
					try {
						idClazz = cl.getMethod("getId").getReturnType();

					}
					catch (NoSuchMethodException e) {
						return fail(cl.getName() + " does not implement the method 'getId()'");
					}

					if (Modifier.isAbstract(idClazz.getModifiers())) {
						try {
							idClazz = cl.getMethod("createId").getReturnType();

						}
						catch (NoSuchMethodException e) {
							return fail(cl.getName() + " does not implement the method 'createId()' unable to retrieve specific id class");
						}
					}

					String packageString = "com.bakdata.conquery.models.identifiable.ids.specific.";
					if (!idClazz.getName().startsWith(packageString)) {
						return fail("The id class " + idClazz + " is not located in the package " + packageString + ". Please clean that up.");
					}

					return Arguments.of(
							cl,
							idClazz
					);
				});
	}
	
	@Test
	public void testEquals() {
		ConceptTreeChildId idA = new ConceptTreeChildId(
			new ConceptTreeChildId(
				new ConceptId(
					new DatasetId("1"),
					"2"
				),
				"3"
			),
			"4"
		);

		ConceptTreeChildId idB = new ConceptTreeChildId(
			new ConceptTreeChildId(
				new ConceptId(
					new DatasetId("1"),
					"2"
				),
				"3"
			),
			"4"
		);

		assertThat(idA).isEqualTo(idB);
		assertThat(idA).hasSameHashCodeAs(idB);
		assertThat(idA.toString()).isEqualTo(idB.toString());
	}
	
	@Test
	public void testStringSerialization() {
		ConceptTreeChildId id = new ConceptTreeChildId(
			new ConceptTreeChildId(
				new ConceptId(
					new DatasetId("1"),
					"2"
				),
				"3"
			),
			"4"
		);

		ConceptTreeChildId copy = ConceptTreeChildId.Parser.INSTANCE.parse(id.toString());

		assertThat(copy).isEqualTo(id);
		assertThat(copy).hasSameHashCodeAs(id);
		assertThat(copy.toString()).isEqualTo(id.toString());
	}
	
	@Test
	public void testJacksonSerialization() throws IOException {
		ConceptTreeChildId id = new ConceptTreeChildId(
			new ConceptTreeChildId(
				new ConceptId(
					new DatasetId("1"),
					"2"
				),
				"3"
			),
			"4"
		);

		ObjectMapper mapper = Jackson.MAPPER.copy();
		STORAGE.injectInto(mapper);

		ConceptTreeChildId copy = mapper.readValue(mapper.writeValueAsBytes(id), ConceptTreeChildId.class);

		assertThat(copy).isEqualTo(id);
		assertThat(copy).hasSameHashCodeAs(id);
		assertThat(copy.toString()).isEqualTo(id.toString());
	}
	
	@Test
	public void testInterning() throws IOException {

		InternalMapperFactory internalMapperFactory = new InternalMapperFactory(new ConqueryConfig(), Validators.newValidator());
		ObjectMapper objectMapper = Jackson.copyMapperAndInjectables(Jackson.MAPPER);
		internalMapperFactory.customizeApiObjectMapper(objectMapper, mock(DatasetRegistry.class), new NonPersistentStoreFactory().createMetaStorage());

		STORAGE.injectInto(objectMapper); // DatasetRegistry-mock doesn't properly inject itself


		ObjectReader objectReader = objectMapper.readerFor(ConceptTreeChildId.class);

		String raw = "\"1.concepts.2.3.4\"";

		ConceptTreeChildId id1 = objectReader.readValue(raw);
		ConceptTreeChildId id2 = objectReader.readValue(raw);

		assertThat(id1).isSameAs(id2);
		assertThat(id1.getParent()).isSameAs(id2.getParent());
		assertThat(id1.findConcept()).isSameAs(id2.findConcept());
		assertThat(id1.findConcept().getDataset()).isSameAs(id2.findConcept().getDataset());
	}
	
	@Test
	public void testJacksonBinarySerialization() throws IOException {
		ConceptTreeChildId id = new ConceptTreeChildId(
			new ConceptTreeChildId(
				new ConceptId(
					new DatasetId("1"),
					"2"
				),
				"3"
			),
			"4"
		);

		ObjectMapper mapper = Jackson.BINARY_MAPPER.copy();
		STORAGE.injectInto(mapper);

		ConceptTreeChildId copy = mapper.readValue(mapper.writeValueAsBytes(id), ConceptTreeChildId.class);

		assertThat(copy).isEqualTo(id);
		assertThat(copy).hasSameHashCodeAs(id);
		assertThat(copy.toString()).isEqualTo(id.toString());
	}

	@ParameterizedTest
	@MethodSource
	public void reflectionTest(Class<?> modelClass, Class<? extends Id<?,?>> expectedIdClass) {

		Class<? extends Id<?,?>> idClass = IdUtil.findIdClass(modelClass);
		assertThat(idClass).isSameAs(expectedIdClass);
		assertThat(IdUtil.createParser(idClass)).isInstanceOf(Parser.class);
	}
}
