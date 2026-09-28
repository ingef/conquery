package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.compiler.dialect.hana.HanaCompilerDialect;
import com.bakdata.conquery.sql.compiler.ColumnRole;
import com.bakdata.conquery.sql.compiler.CompiledColumn;
import com.bakdata.conquery.sql.compiler.CompiledQuery;
import com.bakdata.conquery.sql.model.result.ResultType;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class SqlExtractionQueryTest {

	@Test
	void shouldAdaptValidatedCompilerOutputToTheExistingSqlQueryContract() {
		ResolvedQuery query = validAllEntitiesQuery();
		ResolvedQueryAdapter adapter = new ResolvedQueryAdapter(
				Validation.buildDefaultValidatorFactory().getValidator(),
				(resolved, dialect) -> new CompiledQuery(
						"select entity_id from entities",
						List.of(new CompiledColumn(
								"entity-id", "entity_id", ResultType.Primitive.STRING, ColumnRole.ENTITY_ID
						))
				)
		);

		var adapted = adapter.compile(query, new HanaCompilerDialect(), List.of());

		assertEquals("select entity_id from entities", adapted.getSql());
		assertEquals(List.of(), adapted.getResultInfos());
	}

	@Test
	void shouldRejectInvalidResolvedInputBeforeInvokingTheCompiler() {
		AtomicBoolean compilerInvoked = new AtomicBoolean();
		ResolvedQueryAdapter adapter = new ResolvedQueryAdapter(
				Validation.buildDefaultValidatorFactory().getValidator(),
				(query, dialect) -> {
					compilerInvoked.set(true);
					throw new AssertionError("compiler must not be invoked");
				}
		);
		SqlTable entities = SqlTable.of("entities", "entities");
		ResolvedQuery invalid = new ResolvedQuery(
				new EntitySchema(new ResolvedColumn(
						"entity-id", entities, "id", ColumnType.INTEGER, false
				)),
				new AllEntitiesNode(),
				false,
				List.of()
		);

		assertThrows(ConstraintViolationException.class, () -> adapter.compile(
				invalid, new HanaCompilerDialect(), List.of()
		));
		assertFalse(compilerInvoked.get());
	}

	private static ResolvedQuery validAllEntitiesQuery() {
		SqlTable entities = SqlTable.of("entities", "entities");
		return new ResolvedQuery(
				new EntitySchema(new ResolvedColumn(
						"entity-id", entities, "id", ColumnType.STRING, false
				)),
				new AllEntitiesNode(),
				false,
				List.of()
		);
	}
}
