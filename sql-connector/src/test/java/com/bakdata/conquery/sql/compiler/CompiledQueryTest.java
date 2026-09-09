package com.bakdata.conquery.sql.compiler;

import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertInvalid;
import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.sql.model.result.ResultType;
import org.junit.jupiter.api.Test;

class CompiledQueryTest {

	private static final CompiledColumn ENTITY_ID = new CompiledColumn(
			"entity-id", "entity_id", ResultType.Primitive.STRING, ColumnRole.ENTITY_ID
	);
	private static final CompiledColumn RESULT = new CompiledColumn(
			"concept.exists", "concept_exists", ResultType.Primitive.BOOLEAN, ColumnRole.RESULT
	);

	@Test
	void shouldDescribeTheOrderedPhysicalResult() {
		CompiledQuery query = new CompiledQuery("select entity_id, concept_exists from result", List.of(ENTITY_ID, RESULT));

		assertValid(query);
		assertEquals(List.of(ENTITY_ID, RESULT), query.columns());
	}

	@Test
	void shouldDefensivelyCopyColumns() {
		List<CompiledColumn> columns = new ArrayList<>(List.of(ENTITY_ID));
		CompiledQuery query = new CompiledQuery("select entity_id from result", columns);

		columns.clear();

		assertEquals(List.of(ENTITY_ID), query.columns());
		assertThrows(UnsupportedOperationException.class, () -> query.columns().clear());
	}

	@Test
	void shouldRejectIncompleteOutputContracts() {
		assertInvalid(new CompiledColumn("", "entity_id", ResultType.Primitive.STRING, ColumnRole.ENTITY_ID));
		assertInvalid(new CompiledQuery("", List.of(ENTITY_ID)));
		assertInvalid(new CompiledQuery("select 1", List.of()));
	}

	@Test
	void shouldRejectAmbiguousColumnMappings() {
		CompiledColumn duplicateOutputId = new CompiledColumn(
				ENTITY_ID.outputId(), "other_id", ResultType.Primitive.STRING, ColumnRole.RESULT
		);
		CompiledColumn duplicateAlias = new CompiledColumn(
				"other-id", ENTITY_ID.sqlAlias(), ResultType.Primitive.STRING, ColumnRole.RESULT
		);

		assertInvalid(new CompiledQuery("select entity_id, other_id from result", List.of(ENTITY_ID, duplicateOutputId)));
		assertInvalid(new CompiledQuery("select entity_id, entity_id from result", List.of(ENTITY_ID, duplicateAlias)));
	}
}
