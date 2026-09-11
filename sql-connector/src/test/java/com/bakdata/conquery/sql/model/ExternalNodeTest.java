package com.bakdata.conquery.sql.model;

import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertInvalid;
import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.model.node.ExternalEntity;
import com.bakdata.conquery.sql.model.node.ExternalNode;
import com.bakdata.conquery.sql.model.range.DateRange;
import org.junit.jupiter.api.Test;

class ExternalNodeTest {

	private static final DateRange VALIDITY = DateRange.closed(
			LocalDate.of(2025, 1, 1),
			LocalDate.of(2025, 1, 31)
	);

	@Test
	void shouldAcceptFullyResolvedExternalValues() {
		ExternalNode externalNode = new ExternalNode(
				List.of(entity("person-1", Map.of("group", List.of("A", "B")))),
				List.of("group")
		);

		assertValid(externalNode);
	}

	@Test
	void shouldDefensivelyCopyNestedValues() {
		List<String> columnValues = new ArrayList<>(List.of("A"));
		Map<String, List<String>> values = new LinkedHashMap<>();
		values.put("group", columnValues);
		ExternalEntity entity = new ExternalEntity("person-1", List.of(VALIDITY), values);

		columnValues.add("B");
		values.put("other", List.of("value"));

		assertEquals(Map.of("group", List.of("A")), entity.values());
		assertThrows(UnsupportedOperationException.class, () -> entity.values().put("other", List.of()));
		assertThrows(UnsupportedOperationException.class, () -> entity.values().get("group").add("B"));
	}

	@Test
	void shouldRejectDuplicateEntityIds() {
		assertInvalid(new ExternalNode(
				List.of(entity("person-1", Map.of()), entity("person-1", Map.of())),
				List.of()
		));
	}

	@Test
	void shouldRejectDuplicateValueColumns() {
		assertInvalid(new ExternalNode(
				List.of(entity("person-1", Map.of("group", List.of("A")))),
				List.of("group", "group")
		));
	}

	@Test
	void shouldRejectUndeclaredValueColumns() {
		assertInvalid(new ExternalNode(
				List.of(entity("person-1", Map.of("undeclared", List.of("A")))),
				List.of("group")
		));
	}

	private static ExternalEntity entity(String id, Map<String, List<String>> values) {
		return new ExternalEntity(id, List.of(VALIDITY), values);
	}
}
