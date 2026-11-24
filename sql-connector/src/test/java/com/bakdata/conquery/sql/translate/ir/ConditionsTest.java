package com.bakdata.conquery.sql.translate.ir;

import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.sql.model.range.NumberRange;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class ConditionsTest {

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
	void shouldMatchAnySelectedFlag() {
		assertEquals(
				condition(FIRST_FLAG).isTrue().or(condition(SECOND_FLAG).isTrue()),
				Conditions.flags(List.of(FIRST_FLAG, SECOND_FLAG))
		);
		assertThrows(IllegalArgumentException.class, () -> Conditions.flags(List.of()));
	}

	@Test
	void shouldMatchSelectedStringValuesWithoutADialectService() {
		assertConditionEquals(VALUE.eq("A"), Conditions.stringValues(VALUE, new String[]{"A"}));
		assertConditionEquals(VALUE.in("A", "B"), Conditions.stringValues(VALUE, new String[]{"A", "B"}));
		assertConditionEquals(VALUE.isNull(), Conditions.stringValues(VALUE, new String[]{null, ""}));
	}

	@Test
	void shouldIncludeNullValuesWhenNegatingAStringValueCondition() {
		assertConditionEquals(
				not(VALUE.eq("A")).or(VALUE.isNull()),
				Conditions.negatedStringValues(VALUE, new String[]{"A"})
		);
	}

	@Test
	void shouldMatchStrictlyOverlappingDateRanges() {
		final Condition expected = DATE_RESTRICTION.getStart().lessThan(VALIDITY_DATE.getEnd())
				.and(DATE_RESTRICTION.getEnd().greaterThan(VALIDITY_DATE.getStart()));

		assertConditionEquals(expected, Conditions.dateRestriction(DATE_RESTRICTION, VALIDITY_DATE));
	}

	@Test
	void shouldMatchInclusiveRangeBounds() {
		final Field<BigDecimal> value = field(name("number"), BigDecimal.class);

		assertConditionEquals(
				value.greaterOrEqual(BigDecimal.ONE).and(value.lessOrEqual(BigDecimal.TEN)),
				Conditions.inclusiveRange(value, NumberRange.closed(1, 10))
		);
		assertConditionEquals(value.greaterOrEqual(BigDecimal.ONE), Conditions.inclusiveRange(value, NumberRange.atLeast(1)));
		assertConditionEquals(value.lessOrEqual(BigDecimal.TEN), Conditions.inclusiveRange(value, NumberRange.atMost(10)));
		assertConditionEquals(DSL.noCondition(), Conditions.inclusiveRange(value, NumberRange.unbounded()));
	}

	@Test
	void shouldGroupConditionsByCompilationPhase() {
		final Condition condition = FIRST_FLAG.isTrue();
		final WhereClauses clauses = WhereClauses.builder()
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
