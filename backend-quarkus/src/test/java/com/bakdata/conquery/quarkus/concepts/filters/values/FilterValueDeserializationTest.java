package com.bakdata.conquery.quarkus.concepts.filters.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.math.BigDecimal;
import java.util.Set;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.MultiSelectFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.*;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class FilterValueDeserializationTest {

	@Inject
	ObjectMapper objectMapper;

	@ParameterizedTest
	@ValueSource(strings = {"MULTI_SELECT", "BIG_MULTI_SELECT", "SELECT", "STRING", "INTEGER", "INTEGER_RANGE", "MONEY_RANGE", "REAL", "REAL_RANGE"})
	void deserializesEveryRegisteredType(String type) throws Exception {
		String value = switch (type) {
			case "MULTI_SELECT", "BIG_MULTI_SELECT" -> "[\"one\"]";
			case "SELECT", "STRING" -> "\"one\"";
			case "INTEGER" -> "42";
			case "INTEGER_RANGE", "MONEY_RANGE" -> "{\"min\":1,\"max\":2}";
			case "REAL" -> "1.5";
			case "REAL_RANGE" -> "{\"min\":1.5,\"max\":2.5}";
			default -> throw new IllegalArgumentException(type);
		};
		FilterValue filterValue = read(type, value);

		assertEquals(expectedClass(type), filterValue.getClass());
		assertEquals(FilterId.parse("demo.concept.connector.filter"), filterValue.filter());
		assertEquals(type, objectMapper.valueToTree(filterValue).path("type").asText());
	}

	@Test
	void keepsFilterDefinitionAndFilterValueSelectDiscriminatorsIndependent() throws Exception {
		FilterDefinition definition = objectMapper.readValue("""
				{"type":"SELECT","column":"code"}
				""", FilterDefinition.class);
		FilterValue value = read("SELECT", "\"one\"");

		assertInstanceOf(MultiSelectFilterDefinition.class, definition);
		SelectFilterValue selectValue = assertInstanceOf(SelectFilterValue.class, value);
		assertEquals("one", selectValue.value());
	}

	@Test
	void usesConcreteCollectionNumberAndRangeTypes() throws Exception {
		MultiSelectFilterValue multi = assertInstanceOf(MultiSelectFilterValue.class, read("MULTI_SELECT", "[\"one\",\"two\"]"));
		RealFilterValue real = assertInstanceOf(RealFilterValue.class, read("REAL", "1.25"));
		IntegerRangeFilterValue range = assertInstanceOf(IntegerRangeFilterValue.class, read("INTEGER_RANGE", "{\"min\":1,\"max\":4}"));

		assertEquals(Set.of("one", "two"), multi.value());
		assertEquals(new BigDecimal("1.25"), real.value());
		assertEquals(1L, range.value().min());
		assertEquals(4L, range.value().max());
	}

	private FilterValue read(String type, String value) throws Exception {
		return objectMapper.readValue("{\"type\":\"" + type + "\",\"filter\":\"demo.concept.connector.filter\",\"value\":" + value + "}", FilterValue.class);
	}

	private Class<? extends FilterValue> expectedClass(String type) {
		return switch (type) {
			case "MULTI_SELECT" -> MultiSelectFilterValue.class;
			case "BIG_MULTI_SELECT" -> BigMultiSelectFilterValue.class;
			case "SELECT" -> SelectFilterValue.class;
			case "STRING" -> StringFilterValue.class;
			case "INTEGER" -> IntegerFilterValue.class;
			case "INTEGER_RANGE" -> IntegerRangeFilterValue.class;
			case "MONEY_RANGE" -> MoneyRangeFilterValue.class;
			case "REAL" -> RealFilterValue.class;
			case "REAL_RANGE" -> RealRangeFilterValue.class;
			default -> throw new IllegalArgumentException(type);
		};
	}
}
