package com.bakdata.conquery.models.identifiable.ids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Mappers;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId.Parser;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

		ObjectMapper mapper = Mappers.getMapper();
		ConceptTreeChildId copy = mapper.readValue(mapper.writeValueAsBytes(id), ConceptTreeChildId.class);

		assertThat(copy).isEqualTo(id)
						.hasSameHashCodeAs(id)
						.isEqualTo(id.toString());
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

		ObjectMapper mapper = Mappers.getBinaryMapper();
		ConceptTreeChildId copy = mapper.readValue(mapper.writeValueAsBytes(id), ConceptTreeChildId.class);

		assertThat(copy).isEqualTo(id);
		assertThat(copy).hasSameHashCodeAs(id);
		assertThat(copy.toString()).isEqualTo(id.toString());
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
					Class<?> current = cl;
					while (current != null) {
						String name = current.getSimpleName();


						//special exceptions for this test
						if (name.endsWith("Information")) {
							name = name.substring(0, name.length() - 11);
						}
						if (name.endsWith("Permission")) {
							name = "Permission";
						}

						try {
							return Arguments.of(
									cl,
									Class.forName("com.bakdata.conquery.models.identifiable.ids.specific." + name + "Id")
							);
						}
						catch (ClassNotFoundException e) {
							current = current.getSuperclass();
						}
					}

					return fail("Could not find id class for " + cl);
				});
	}

	@ParameterizedTest
	@MethodSource
	public void reflectionTest(Class<?> modelClass, Class<? extends IId<?>> expectedIdClass) {

		Class<? extends IId<?>> idClass = IId.findIdClass(modelClass);
		assertThat(idClass).isSameAs(expectedIdClass);
		assertThat(IId.createParser(idClass)).isInstanceOf(Parser.class);
	}
}
