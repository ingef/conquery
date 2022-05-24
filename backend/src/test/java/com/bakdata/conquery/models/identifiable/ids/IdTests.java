package com.bakdata.conquery.models.identifiable.ids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.AId.Parser;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IdTests {

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
	public void testJacksonSerialization() throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
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
		
		ObjectMapper mapper = Jackson.MAPPER;
		ConceptTreeChildId copy = mapper.readValue(mapper.writeValueAsBytes(id), ConceptTreeChildId.class);
		
		assertThat(copy).isEqualTo(id);
		assertThat(copy).hasSameHashCodeAs(id);
		assertThat(copy.toString()).isEqualTo(id.toString());
	}
	
	@Test
	public void testInterning() throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
		String raw = "1.concepts.2.3.4";
		
		ConceptTreeChildId id1 = ConceptTreeChildId.Parser.INSTANCE.parse(raw);
		ConceptTreeChildId id2 = ConceptTreeChildId.Parser.INSTANCE.parse(raw);
		
		assertThat(id1).isSameAs(id2);
		assertThat(id1.getParent()).isSameAs(id2.getParent());
		assertThat(id1.findConcept()).isSameAs(id2.findConcept());
		assertThat(id1.findConcept().getDataset()).isSameAs(id2.findConcept().getDataset());
	}
	
	@Test
	public void testJacksonBinarySerialization() throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
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
		
		ObjectMapper mapper = Jackson.BINARY_MAPPER;
		ConceptTreeChildId copy = mapper.readValue(mapper.writeValueAsBytes(id), ConceptTreeChildId.class);
		
		assertThat(copy).isEqualTo(id);
		assertThat(copy).hasSameHashCodeAs(id);
		assertThat(copy.toString()).isEqualTo(id.toString());
	}
	
	public static Stream<Arguments> reflectionTest() {
		final Class<Identifiable> identifiableClass = Identifiable.class;
		return CPSTypeIdResolver
				.SCAN_RESULT
				.getClassesImplementing(identifiableClass.getName()).loadClasses()
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
						return fail(" does not implement the method 'getId()'");
					}

					if (Modifier.isAbstract(idClazz.getModifiers())) {
						try {
							idClazz = cl.getMethod("createId").getReturnType();

						}
						catch (NoSuchMethodException e) {
							return fail(cl + " does not implement the method 'createId()' unable to retrieve specific id class");
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

	@ParameterizedTest
	@MethodSource
	public void reflectionTest(Class<?> modelClass, Class<? extends AId<?>> expectedIdClass) {

		Class<? extends AId<?>> idClass = AId.findIdClass(modelClass);
		assertThat(idClass).isSameAs(expectedIdClass);
		assertThat(AId.createParser(idClass)).isInstanceOf(Parser.class);
	}
}
