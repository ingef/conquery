package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.compiler.ColumnRole;
import com.bakdata.conquery.sql.compiler.CompiledColumn;
import com.bakdata.conquery.sql.compiler.CompiledQuery;
import com.bakdata.conquery.sql.compiler.dialect.hana.HanaCompilerDialect;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.result.ResultType;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
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
				),
				mock(ResolvedQueryMapper.class)
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
				},
				mock(ResolvedQueryMapper.class)
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

	@Test
	void shouldMapAConceptQueryWithOneResultInfoSnapshot() {
		ConceptQuery query = mock(ConceptQuery.class);
		ResultInfo resultInfo = mock(ResultInfo.class);
		List<ResultInfo> resultInfos = List.of(resultInfo);
		ResolvedQuery resolved = validAllEntitiesQuery();
		ResolvedQueryMapper mapper = mock(ResolvedQueryMapper.class);
		when(query.getResultInfos()).thenReturn(resultInfos);
		when(mapper.map(same(query), eq(Optional.empty()), same(resultInfos))).thenReturn(resolved);
		ResolvedQueryAdapter adapter = new ResolvedQueryAdapter(
				Validation.buildDefaultValidatorFactory().getValidator(),
				(ignored, dialect) -> compiledWithOneResult(),
				mapper
		);

		var adapted = adapter.compile(query, new HanaCompilerDialect());

		assertEquals(resultInfos, adapted.getResultInfos());
		verify(query, times(1)).getResultInfos();
		verify(mapper).map(same(query), eq(Optional.empty()), same(resultInfos));
	}

	@Test
	void shouldMapASecondaryIdQueryWithItsOuterResultInfos() {
		SecondaryIdQuery query = mock(SecondaryIdQuery.class);
		ConceptQuery nested = mock(ConceptQuery.class);
		SecondaryIdDescriptionId secondaryId = mock(SecondaryIdDescriptionId.class);
		SecondaryIdDescription resolvedSecondaryId = mock(SecondaryIdDescription.class);
		ResultInfo resultInfo = mock(ResultInfo.class);
		List<ResultInfo> resultInfos = List.of(resultInfo);
		ResolvedQuery resolved = validAllEntitiesQuery();
		ResolvedQueryMapper mapper = mock(ResolvedQueryMapper.class);
		when(query.getQuery()).thenReturn(nested);
		when(query.getSecondaryId()).thenReturn(secondaryId);
		when(secondaryId.resolve()).thenReturn(resolvedSecondaryId);
		when(query.getResultInfos()).thenReturn(resultInfos);
		when(mapper.map(same(nested), eq(Optional.of(resolvedSecondaryId)), same(resultInfos))).thenReturn(resolved);
		ResolvedQueryAdapter adapter = new ResolvedQueryAdapter(
				Validation.buildDefaultValidatorFactory().getValidator(),
				(ignored, dialect) -> compiledWithOneResult(),
				mapper
		);

		var adapted = adapter.compile(query, new HanaCompilerDialect());

		assertEquals(resultInfos, adapted.getResultInfos());
		verify(query, times(1)).getResultInfos();
		verify(mapper).map(same(nested), eq(Optional.of(resolvedSecondaryId)), same(resultInfos));
	}

	private static CompiledQuery compiledWithOneResult() {
		return new CompiledQuery(
				"select entity_id, result from entities",
				List.of(
						new CompiledColumn("entity-id", "entity_id", ResultType.Primitive.STRING, ColumnRole.ENTITY_ID),
						new CompiledColumn("result", "result", ResultType.Primitive.STRING, ColumnRole.RESULT)
				)
		);
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
