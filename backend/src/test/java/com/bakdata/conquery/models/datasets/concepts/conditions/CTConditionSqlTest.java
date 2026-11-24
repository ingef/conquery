package com.bakdata.conquery.models.datasets.concepts.conditions;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class CTConditionSqlTest {

	private static final Field<String> VALUE = field(name("value"), String.class);
	private static final CTConditionContext CONTEXT = new CTConditionContext(name("value"), null, true);

	@Test
	void shouldIncludeNullValuesWhenNegatingEquality() {
		final NotCondition condition = new NotCondition();
		condition.setCondition(EqualCondition.create(Set.of("A")));

		assertConditionEquals(not(VALUE.eq("A")).or(VALUE.isNull()), condition.convertToSqlCondition(CONTEXT));
	}

	@Test
	void shouldRestoreEqualityWhenNegatingTwice() {
		final NotCondition inner = new NotCondition();
		inner.setCondition(EqualCondition.create(Set.of("A")));
		final NotCondition outer = new NotCondition();
		outer.setCondition(inner);

		assertConditionEquals(not(not(VALUE.eq("A")).or(VALUE.isNull())), outer.convertToSqlCondition(CONTEXT));
	}

	@Test
	void shouldPreserveNullAwareNegationThroughASingletonConjunction() {
		final AndCondition conjunction = new AndCondition();
		conjunction.setConditions(List.of(EqualCondition.create(Set.of("A"))));
		final NotCondition condition = new NotCondition();
		condition.setCondition(conjunction);

		assertConditionEquals(not(VALUE.eq("A")).or(VALUE.isNull()), condition.convertToSqlCondition(CONTEXT));
	}

	@Test
	void shouldApplyDoubleNegationToTheConvertedEmptyValueCondition() {
		final NotCondition inner = new NotCondition();
		inner.setCondition(EqualCondition.create(Set.of("")));
		final NotCondition outer = new NotCondition();
		outer.setCondition(inner);

		assertConditionEquals(not(not(VALUE.isNull()).or(VALUE.isNull())), outer.convertToSqlCondition(CONTEXT));
	}

	private static void assertConditionEquals(Condition expected, Condition actual) {
		assertEquals(
				DSL.using(SQLDialect.POSTGRES).renderInlined(expected),
				DSL.using(SQLDialect.POSTGRES).renderInlined(actual)
		);
	}
}
