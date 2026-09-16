package com.bakdata.conquery.sql.compiler.ir.select;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class ColumnDateRangeTest {

	@Test
	void shouldAliasValidityDateColumns() {
		ColumnDateRange range = range().asValidityDateRange("event");

		assertEquals("event_validity_date", range.getAlias());
		assertEquals("event_validity_date_start", range.getStart().getName());
		assertEquals("event_validity_date_end", range.getEnd().getName());
		assertEquals(List.of("event_validity_date_start", "event_validity_date_end"), range.requiredColumns());
	}

	@Test
	void shouldQualifyAliasedDateRangeColumns() {
		ColumnDateRange qualified = range().as("event").qualify("query_step");

		assertEquals(name("query_step", "event_start"), qualified.getStart().getQualifiedName());
		assertEquals(name("query_step", "event_end"), qualified.getEnd().getQualifiedName());
		assertEquals("event", qualified.getAlias());
	}

	@Test
	void shouldPreserveLeftAliasWhenCoalescing() {
		ColumnDateRange coalesced = range().as("event").coalesce(range().as("fallback"));

		assertEquals("event", coalesced.getAlias());
		assertEquals("event_start", coalesced.getStart().getName());
		assertEquals("event_end", coalesced.getEnd().getName());
	}

	private static ColumnDateRange range() {
		return ColumnDateRange.of(field(name("start"), Date.class), field(name("end"), Date.class));
	}
}
