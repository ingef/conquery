package com.bakdata.conquery.sql.translate;

import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertInvalid;
import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.sql.model.result.ResultType;
import org.junit.jupiter.api.Test;

class TranslatedQueryTest {

	private static final TranslatedColumn ENTITY_ID = new TranslatedColumn(
			"entity-id", "entity_id", ResultType.Primitive.STRING, ColumnRole.ENTITY_ID
	);
	private static final TranslatedColumn RESULT = new TranslatedColumn(
			"concept.exists", "concept_exists", ResultType.Primitive.BOOLEAN, ColumnRole.RESULT
	);

	@Test
	void shouldDescribeTheOrderedPhysicalResult() {
		TranslatedQuery query = new TranslatedQuery("select entity_id, concept_exists from result", List.of(ENTITY_ID, RESULT));

		assertValid(query);
		assertEquals(List.of(ENTITY_ID, RESULT), query.columns());
	}

	@Test
	void shouldDefensivelyCopyColumns() {
		List<TranslatedColumn> columns = new ArrayList<>(List.of(ENTITY_ID));
		TranslatedQuery query = new TranslatedQuery("select entity_id from result", columns);

		columns.clear();

		assertEquals(List.of(ENTITY_ID), query.columns());
		assertThrows(UnsupportedOperationException.class, () -> query.columns().clear());
	}

	@Test
	void shouldRejectIncompleteOutputContracts() {
		assertInvalid(new TranslatedColumn("", "entity_id", ResultType.Primitive.STRING, ColumnRole.ENTITY_ID));
		assertInvalid(new TranslatedQuery("", List.of(ENTITY_ID)));
		assertInvalid(new TranslatedQuery("select 1", List.of()));
	}

	@Test
	void shouldRejectAmbiguousColumnMappings() {
		TranslatedColumn duplicateOutputId = new TranslatedColumn(
				ENTITY_ID.outputId(), "other_id", ResultType.Primitive.STRING, ColumnRole.RESULT
		);
		TranslatedColumn duplicateAlias = new TranslatedColumn(
				"other-id", ENTITY_ID.sqlAlias(), ResultType.Primitive.STRING, ColumnRole.RESULT
		);

		assertInvalid(new TranslatedQuery("select entity_id, other_id from result", List.of(ENTITY_ID, duplicateOutputId)));
		assertInvalid(new TranslatedQuery("select entity_id, entity_id from result", List.of(ENTITY_ID, duplicateAlias)));
	}
}
