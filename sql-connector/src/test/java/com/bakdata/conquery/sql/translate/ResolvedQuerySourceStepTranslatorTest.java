package com.bakdata.conquery.sql.translate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.ResolvedQuerySource;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import com.bakdata.conquery.sql.translate.dialect.hana.HanaTranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.ProjectionMode;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import org.junit.jupiter.api.Test;

class ResolvedQuerySourceStepTranslatorTest {

	@Test
	void shouldTranslateAUnionToDistinctCanonicalIdsWithUniqueNames() {
		EntitySchema schema = schema();
		ResolvedQuerySource.Standard first = standard(schema);
		ResolvedQuerySource.Standard second = standard(schema);
		HanaTranslatorDialect dialect = new HanaTranslatorDialect();
		QueryStep union = new ResolvedQuerySourceStepTranslator().translate(
				new ResolvedQuerySource.Union(schema, List.of(first, second), false),
				dialect,
				new SqlNameGenerator(dialect.getNameMaxLength()));

		assertTrue(union.isUnion());
		assertFalse(union.isUnionAll());
		assertEquals(2, union.getPredecessors().size());
		assertNotEquals(union.getPredecessors().get(0).getCteName(), union.getPredecessors().get(1).getCteName());
		assertEquals("person_id", union.getSelects().getIds().getPrimaryColumn().getName());
		assertTrue(union.getSelects().getValidityDate().isEmpty());
	}

	@Test
	void shouldKeepTheUniqueSuffixWhenNamesAreTruncated() {
		SqlNameGenerator names = new SqlNameGenerator(16);
		String first = names.reusableStepName("a_very_long_reusable_query_step");
		String second = names.reusableStepName("a_very_long_reusable_query_step");

		assertEquals(16, first.length());
		assertEquals(16, second.length());
		assertTrue(first.endsWith("-source-1"));
		assertTrue(second.endsWith("-source-2"));
	}

	@Test
	void shouldAggregateValidityAfterUnioningChildren() {
		EntitySchema schema = schema();
		HanaTranslatorDialect dialect = new HanaTranslatorDialect();
		ResolvedQuery dated = new ResolvedQuery(schema, new AllEntitiesNode(), true, List.of());
		QueryStep result = new ResolvedQuerySourceStepTranslator().translate(
				new ResolvedQuerySource.Union(
						schema, List.of(new ResolvedQuerySource.Standard(dated)), true),
				dialect,
				new SqlNameGenerator(dialect.getNameMaxLength()));

		assertTrue(result.getSelects().getValidityDate().isPresent());
		assertTrue(result.isGroupBy());
		assertEquals(1, result.getPredecessors().size());
		assertTrue(result.getPredecessors().getFirst().getCteName().startsWith("query_union"));
	}

	@Test
	void shouldAliasEmptyValidityBeforeUnionProjection() {
		EntitySchema schema = schema();
		HanaTranslatorDialect dialect = new HanaTranslatorDialect();
		QueryStep result = new ResolvedQuerySourceStepTranslator().translate(
				new ResolvedQuerySource.Union(schema, List.of(standard(schema)), true),
				dialect,
				new SqlNameGenerator(dialect.getNameMaxLength()));

		QueryStep union = result.getPredecessors().getFirst();
		assertTrue(union.getSelects().getValidityDate().orElseThrow().getStart().getName()
				.startsWith("query_union-source-"));
	}

	@Test
	void shouldKeepStandardSourceValidityAsIntermediateColumns() {
		EntitySchema schema = schema();
		HanaTranslatorDialect dialect = new HanaTranslatorDialect();
		QueryStep result = new ResolvedQuerySourceStepTranslator().translate(
				new ResolvedQuerySource.Standard(new ResolvedQuery(
						schema, new AllEntitiesNode(), true, List.of())),
				dialect,
				new SqlNameGenerator(dialect.getNameMaxLength()));

		assertEquals(ProjectionMode.INTERMEDIATE, result.getProjectionMode());
		assertTrue(result.getSelects().getValidityDate().isPresent());
		assertEquals(2, result.getSelects().getValidityDate().orElseThrow().toFields().size());
		assertTrue(result.getSelects().getValidityDate().orElseThrow().getStart().getName()
				.startsWith("query_result-source-"));
	}

	private static ResolvedQuerySource.Standard standard(EntitySchema schema) {
		return new ResolvedQuerySource.Standard(new ResolvedQuery(schema, new AllEntitiesNode(), false, List.of()));
	}

	private static EntitySchema schema() {
		SqlTable table = SqlTable.of("entities", "catalog", "entities");
		return new EntitySchema(new ResolvedColumn("entity-id", table, "person_id", ColumnType.STRING, false));
	}
}
