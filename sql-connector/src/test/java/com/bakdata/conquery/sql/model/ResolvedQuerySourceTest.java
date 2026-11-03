package com.bakdata.conquery.sql.model;

import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertInvalid;
import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.junit.jupiter.api.Test;

class ResolvedQuerySourceTest {

	@Test
	void shouldValidateAndDefensivelyCopyAQuerySourceUnion() {
		EntitySchema schema = schema("entities");
		List<ResolvedQuerySource> children = new ArrayList<>();
		children.add(standard(schema));
		ResolvedQuerySource.Union union = new ResolvedQuerySource.Union(schema, children, true);

		children.add(standard(schema));

		assertValid(union);
		assertEquals(schema, union.entitySchema());
		assertEquals(1, union.children().size());
		assertThrows(UnsupportedOperationException.class, () -> union.children().clear());
	}

	@Test
	void shouldRejectAQuerySourceUnionWithHeterogeneousEntitySchemas() {
		EntitySchema schema = schema("entities");
		ResolvedQuerySource.Union union = new ResolvedQuerySource.Union(
				schema,
				List.of(standard(schema), standard(schema("other_entities"))),
				false
		);

		assertInvalid(union);
	}

	private static ResolvedQuerySource.Standard standard(EntitySchema schema) {
		return new ResolvedQuerySource.Standard(new ResolvedQuery(schema, new AllEntitiesNode(), false, List.of()));
	}

	private static EntitySchema schema(String tableName) {
		SqlTable table = SqlTable.of(tableName, tableName);
		return new EntitySchema(new ResolvedColumn(
				tableName + ".id", table, "id", ColumnType.STRING, false
		));
	}
}
