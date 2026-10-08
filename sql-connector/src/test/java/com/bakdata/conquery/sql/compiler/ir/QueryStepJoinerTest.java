package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class QueryStepJoinerTest {

	@Test
	void shouldJoinStepsOnIdsAndStratificationDates() {
		QueryStep first = queryStep("first", "first_value");
		QueryStep second = queryStep("second", "second_value");

		String sql = renderJoin(first, second, JoinMode.FULL_OUTER);

		assertTrue(sql.contains("full outer join \"second\""));
		assertTrue(sql.contains("\"first\".\"person\" = \"second\".\"person\""));
		assertTrue(sql.contains("\"first\".\"window_start\" = \"second\".\"window_start\""));
		assertTrue(sql.contains("\"first\".\"window_end\" = \"second\".\"window_end\""));
	}

	@Test
	void shouldRenderEverySupportedJoinMode() {
		QueryStep first = queryStep("first", "first_value");
		QueryStep second = queryStep("second", "second_value");

		assertTrue(renderJoin(first, second, JoinMode.INNER).contains("join \"second\""));
		assertTrue(renderJoin(first, second, JoinMode.FULL_OUTER).contains("full outer join \"second\""));
		assertTrue(renderJoin(first, second, JoinMode.LEFT).contains("left outer join \"second\""));
	}

	@Test
	void shouldMergeSelectsAndCoalesceIds() {
		QueryStep first = queryStep("first", "first_value");
		QueryStep second = queryStep("second", "second_value");

		assertEquals(
				List.of(name("first", "first_value"), name("second", "second_value")),
				QueryStepJoiner.mergeSelects(List.of(first, second)).stream()
						.flatMap(select -> select.toFields().stream())
						.map(org.jooq.Field::getQualifiedName)
						.toList()
		);
		assertEquals(
				SharedAliases.PRIMARY_COLUMN.getAlias(),
				QueryStepJoiner.coalesceIds(List.of(first, second)).getPrimaryColumn().getName()
		);
	}

	@Test
	void shouldRejectEmptyInput() {
		assertThrows(IllegalArgumentException.class, () -> QueryStepJoiner.join(List.of(), JoinMode.INNER));
		assertThrows(IllegalArgumentException.class, () -> QueryStepJoiner.coalesceIds(List.of()));
	}

	private static String renderJoin(QueryStep first, QueryStep second, JoinMode joinMode) {
		return DSL.using(SQLDialect.POSTGRES)
				.select()
				.from(QueryStepJoiner.join(List.of(first, second), joinMode))
				.getSQL()
				.toLowerCase(Locale.ROOT);
	}

	private static QueryStep queryStep(String cteName, String valueColumn) {
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person"), String.class)))
						.stratificationDate(Optional.of(ColumnDateRange.of(
								field(name("window_start"), Date.class),
								field(name("window_end"), Date.class)
						)))
						.sqlSelect(new FieldWrapper<>(field(name(valueColumn), Integer.class)))
						.build())
				.build();
	}
}
