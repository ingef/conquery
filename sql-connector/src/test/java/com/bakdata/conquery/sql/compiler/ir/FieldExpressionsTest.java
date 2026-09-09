package com.bakdata.conquery.sql.compiler.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import java.util.List;

import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class FieldExpressionsTest {

	@Test
	void shouldCreateLeastAndGreatestExpressions() {
		Field<Integer> first = field(name("first"), Integer.class);
		Field<Integer> second = field(name("second"), Integer.class);

		assertEquals("least(\"first\", \"second\")", render(FieldExpressions.least(List.of(first, second))));
		assertEquals("greatest(\"first\", \"second\")", render(FieldExpressions.greatest(List.of(first, second))));
	}

	@Test
	void shouldPreserveEmptyFieldListBehavior() {
		assertNull(FieldExpressions.least(List.of()));
		assertNull(FieldExpressions.greatest(List.of()));
	}

	private static String render(Field<?> expression) {
		return DSL.using(SQLDialect.POSTGRES).renderInlined(expression);
	}
}
