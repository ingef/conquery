package com.bakdata.conquery.sql.compiler.dialect;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.Locale;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class CompilerDialectTest {

	private static final CompilerDialect DIALECT = new TestDialect();

	@Test
	void shouldCreateEmptyDateRange() {
		ColumnDateRange dateRange = DIALECT.emptyDateRange();

		assertEquals("null", render(dateRange.getStart()));
		assertEquals("null", render(dateRange.getEnd()));
	}

	@Test
	void shouldCreateUnboundedDateRangeFromDialectSentinels() {
		ColumnDateRange dateRange = DIALECT.unboundedDateRange();

		assertEquals("\"minimum_date\"", render(dateRange.getStart()));
		assertEquals("\"maximum_date\"", render(dateRange.getEnd()));
	}

	@Test
	void shouldCreateConditionalUnboundedDateRange() {
		ColumnDateRange dateRange = DIALECT.conditionalUnboundedDateRange(
				field(name("include_all"), Boolean.class).isTrue()
		);

		String start = render(dateRange.getStart());
		String end = render(dateRange.getEnd());
		assertTrue(start.contains("case when"));
		assertTrue(start.contains("\"minimum_date\""));
		assertTrue(end.contains("case when"));
		assertTrue(end.contains("\"maximum_date\""));
	}

	private static String render(Field<?> expression) {
		return DSL.using(SQLDialect.POSTGRES).renderInlined(expression).toLowerCase(Locale.ROOT);
	}

	private static final class TestDialect implements CompilerDialect {

		@Override
		public Field<Date> minimumDate() {
			return field(name("minimum_date"), Date.class);
		}

		@Override
		public Field<Date> maximumDate() {
			return field(name("maximum_date"), Date.class);
		}

		@Override
		public <T> Field<T> anyValue(Field<T> value) {
			return value;
		}

		@Override
		public Field<?> renderDateRange(Field<Date> start, Field<Date> end) {
			return field(name("rendered_range"), Object.class);
		}

		@Override
		public Field<?> aggregateDateRanges(Field<Date> start, Field<Date> end) {
			return field(name("aggregated_ranges"), Object.class);
		}

		@Override
		public int getNameMaxLength() {
			return 128;
		}
	}
}
