package com.bakdata.conquery.models.identifiable.ids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IdUtil.Parser;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IdTests {

	private NamespaceStorage storage;

	@BeforeEach
	public void setup() {
		storage = new NonPersistentStoreFactory().createNamespaceStorage();
	}

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
					new DatasetId("1", storage),
					"2"
				),
				"3"
			),
			"4"
		);

		ConceptTreeChildId idB = new ConceptTreeChildId(
			new ConceptTreeChildId(
				new ConceptId(
					new DatasetId("1", storage),
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
					new DatasetId("1", storage),
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
		DatasetId dataset = new DatasetId("1", storage);
		ConceptTreeChildId id = new ConceptTreeChildId(
			new ConceptTreeChildId(
				new ConceptId(
						dataset,
					"2"
				),
				"3"
			),
			"4"
		);

		ObjectMapper mapper = storage.injectInto(Jackson.MAPPER);

		ConceptTreeChildId copy = mapper.readValue(mapper.writeValueAsBytes(id), ConceptTreeChildId.class);

		assertThat(copy).isEqualTo(id);
		assertThat(copy).hasSameHashCodeAs(id);
		assertThat(copy.toString()).isEqualTo(id.toString());
	}
	

	@Test
	public void testJacksonBinarySerialization() throws IOException {

	}

	@ParameterizedTest
	@MethodSource
	public void reflectionTest(Class<?> modelClass, Class<? extends Id<?, ?>> expectedIdClass) {

		Class<? extends Id<?, ?>> idClass = IdUtil.findIdClass(modelClass);
		assertThat(idClass).isSameAs(expectedIdClass);
		assertThat(IdUtil.createParser(idClass)).isInstanceOf(Parser.class);
	}
}
