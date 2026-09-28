package com.bakdata.conquery.sql.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

class ConceptIdMappingResolverTest {

	@Test
	void shouldResolveTheMostSpecificRowsWithExistingDefaultsAndFieldOrder() {
		Field<String> shortCode = DSL.field(DSL.name("code"), SQLDataType.VARCHAR(2));
		Field<String> longCode = DSL.field(DSL.name("code"), SQLDataType.VARCHAR(5));
		Field<Boolean> flag = DSL.field(DSL.name("flag"), Boolean.class);
		Field<String> codeExtractor = DSL.field(DSL.name("events", "code"), String.class);
		Field<Boolean> flagExtractor = DSL.field(DSL.name("events", "flag"), Boolean.class);
		List<ConceptIdMappingResolver.MappingExpression> expressions = List.of(
				new ConceptIdMappingResolver.MappingExpression("root", 0, 0, Map.of()),
				new ConceptIdMappingResolver.MappingExpression("parent", 1, 1, Map.of(
						shortCode, new ConceptIdMappingResolver.FieldCondition(codeExtractor, Set.of(DSL.val("A")))
				)),
				new ConceptIdMappingResolver.MappingExpression("child", 2, 2, Map.of(
						longCode, new ConceptIdMappingResolver.FieldCondition(codeExtractor, Set.of(DSL.val("A"))),
						flag, new ConceptIdMappingResolver.FieldCondition(flagExtractor, Set.of(DSL.val(true)))
				))
		);

		ConceptIdMappingTable mapping = ConceptIdMappingResolver.resolve(
				"diagnosis", DSL.name("diagnosis_ids"), expressions
		);

		assertEquals(List.of("code", "flag"), mapping.keyFields().stream().map(Field::getName).toList());
		assertEquals(5, mapping.keyFields().getFirst().getDataType().length());
		assertEquals(Set.of(
				Arrays.asList(0, null, false),
				List.of(1, "A", false),
				List.of(2, "A", true)
		), mapping.rows().stream().map(row -> Arrays.stream(row.fields())
				.map(field -> ((Param<?>) field).getValue()).toList()).collect(java.util.stream.Collectors.toSet()));
	}

	@Test
	void shouldRejectOverlappingSiblingMappings() {
		Field<String> code = DSL.field(DSL.name("code"), String.class);
		Field<String> extractor = DSL.field(DSL.name("events", "code"), String.class);
		ConceptIdMappingResolver.FieldCondition condition = new ConceptIdMappingResolver.FieldCondition(
				extractor, Set.of(DSL.val("A"))
		);

		assertThrows(IllegalArgumentException.class, () -> ConceptIdMappingResolver.resolve(
				"diagnosis",
				DSL.name("diagnosis_ids"),
				List.of(
						new ConceptIdMappingResolver.MappingExpression("left", 1, 1, Map.of(code, condition)),
						new ConceptIdMappingResolver.MappingExpression("right", 2, 1, Map.of(code, condition))
				)
		));
	}
}
