package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class SqlIdColumnsTest {

	@Test
	void shouldAliasIdColumnsAndRetainTheirPredecessor() {
		SqlIdColumns original = new SqlIdColumns(
				field(name("source", "person"), String.class),
				field(name("source", "visit"), String.class)
		);

		SqlIdColumns aliased = original.withAlias();

		assertEquals(SharedAliases.PRIMARY_COLUMN.getAlias(), aliased.getPrimaryColumn().getName());
		assertEquals(SharedAliases.SECONDARY_ID.getAlias(), aliased.getSecondaryId().orElseThrow().getName());
		assertSame(original, aliased.getPredecessor().orElseThrow());
	}

	@Test
	void shouldQualifyIdColumns() {
		SqlIdColumns ids = new SqlIdColumns(field(name("person"), String.class));

		SqlIdColumns qualified = ids.qualify("query_step");

		assertEquals(name("query_step", "person"), qualified.getPrimaryColumn().getQualifiedName());
		assertSame(ids, qualified.getPredecessor().orElseThrow());
	}

	@Test
	void shouldCreateStratifiedIdColumnsFromNormalizedResolution() {
		SqlIdColumns ids = new SqlIdColumns(field(name("person"), String.class));

		SqlIdColumns stratified = ids.withStratification(
				"COMPLETE",
				field(name(SharedAliases.INDEX.getAlias()), Integer.class),
				field(name(SharedAliases.INDEX_SELECTOR.getAlias()), Date.class)
		);

		assertTrue(stratified.isWithStratification());
		assertEquals(
				List.of(
						"person",
						SharedAliases.RESOLUTION.getAlias(),
						SharedAliases.INDEX.getAlias(),
						SharedAliases.INDEX_SELECTOR.getAlias()
				),
				stratified.toFields().stream().map(org.jooq.Field::getName).toList()
		);
		assertEquals(SharedAliases.INDEX.getAlias(), stratified.forFinalSelect("COMPLETE").toFields().get(2).getName());
	}

	@Test
	void shouldRejectCoalescingStratifiedAndPlainIds() {
		SqlIdColumns plain = new SqlIdColumns(field(name("person"), String.class));
		SqlIdColumns stratified = plain.withStratification("YEARS", field(name("index"), Integer.class));

		assertThrows(IllegalArgumentException.class, () -> stratified.coalesce(List.of(plain)));
	}
}
