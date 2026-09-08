package com.bakdata.conquery.sql.compiler.ir.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.not;

import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class WhereConditionTest {

	private static final Field<Boolean> FIRST_FLAG = field(name("first_flag"), Boolean.class);
	private static final Field<Boolean> SECOND_FLAG = field(name("second_flag"), Boolean.class);
	private static final Field<String> VALUE = field(name("value"), String.class);
	private static final ColumnDateRange DATE_RESTRICTION = ColumnDateRange.of(
			field(name("restriction_start"), Date.class), field(name("restriction_end"), Date.class)
	);
	private static final ColumnDateRange VALIDITY_DATE = ColumnDateRange.of(
			field(name("validity_start"), Date.class), field(name("validity_end"), Date.class)
	);

	@Test
	void shouldComposeAndInvertConditions() {
		WhereCondition first = new ConditionWrappingWhereCondition(FIRST_FLAG.isTrue());
		WhereCondition second = new ConditionWrappingWhereCondition(SECOND_FLAG.isTrue());

		assertEquals(FIRST_FLAG.isTrue().and(SECOND_FLAG.isTrue()), first.and(second).condition());
		assertEquals(FIRST_FLAG.isTrue().or(SECOND_FLAG.isTrue()), first.or(second).condition());
		assertEquals(not(FIRST_FLAG.isTrue()), first.negate().condition());
		assertSame(first, first.negate().negate());
	}

	@Test
	void shouldMatchAnySelectedFlag() {
		FlagCondition flags = new FlagCondition(List.of(FIRST_FLAG, SECOND_FLAG));

		assertEquals(condition(FIRST_FLAG).isTrue().or(condition(SECOND_FLAG).isTrue()), flags.condition());
		assertThrows(IllegalArgumentException.class, () -> new FlagCondition(List.of()).condition());
	}

	@Test
	void shouldMatchSelectedStringValuesWithoutADialectService() {
		assertConditionEquals(VALUE.eq("A"), new StringValuesCondition(VALUE, new String[]{"A"}).condition());
		assertConditionEquals(VALUE.in("A", "B"), new StringValuesCondition(VALUE, new String[]{"A", "B"}).condition());
		assertConditionEquals(VALUE.isNull(), new StringValuesCondition(VALUE, new String[]{null, ""}).condition());
	}

	@Test
	void shouldIncludeNullValuesWhenNegatingAStringValueCondition() {
		WhereCondition condition = new StringValuesCondition(VALUE, new String[]{"A"});

		assertConditionEquals(not(VALUE.eq("A")).or(VALUE.isNull()), condition.negate().condition());
	}

	@Test
	void shouldMatchStrictlyOverlappingDateRanges() {
		Condition expected = DATE_RESTRICTION.getStart().lessThan(VALIDITY_DATE.getEnd())
				.and(DATE_RESTRICTION.getEnd().greaterThan(VALIDITY_DATE.getStart()));

		assertConditionEquals(expected, new DateRestrictionCondition(DATE_RESTRICTION, VALIDITY_DATE).condition());
	}

	@Test
	void shouldGroupConditionsByCompilationPhase() {
		WhereCondition condition = new ConditionWrappingWhereCondition(FIRST_FLAG.isTrue());
		WhereClauses clauses = WhereClauses.builder()
				.preprocessingCondition(condition)
				.eventFilter(condition)
				.groupFilter(condition)
				.build();

		assertEquals(List.of(condition), clauses.getPreprocessingConditions());
		assertEquals(List.of(condition), clauses.getEventFilters());
		assertEquals(List.of(condition), clauses.getGroupFilters());
		assertTrue(WhereClauses.empty().getPreprocessingConditions().isEmpty());
		assertTrue(WhereClauses.empty().getEventFilters().isEmpty());
		assertTrue(WhereClauses.empty().getGroupFilters().isEmpty());
	}

	private static void assertConditionEquals(Condition expected, Condition actual) {
		assertEquals(
				DSL.using(SQLDialect.POSTGRES).renderInlined(expected),
				DSL.using(SQLDialect.POSTGRES).renderInlined(actual)
		);
	}
}
