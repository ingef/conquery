package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class FinalQueryStepComposerTest {

	private static final CompilerDialect DIALECT = new TestDialect();

	@Test
	void shouldBuildAggregatedFinalStep() {
		QueryStep preFinalStep = queryStep("compiled", "value", true);
		TestDialect dialect = new TestDialect();

		QueryStep result = FinalQueryStepComposer.compose(preFinalStep, Optional.empty(), true, dialect);

		assertNull(result.getCteName());
		assertEquals(ProjectionMode.AGGREGATED, result.getProjectionMode());
		assertEquals(List.of(preFinalStep), result.getPredecessors());
		assertEquals(List.of(name("compiled", "person_id")), result.getGroupBy().stream()
				.map(Field::getQualifiedName)
				.toList());
		assertTrue(result.getSelects().getValidityDate().isPresent());
		assertEquals(1, dialect.getAnyValueCalls());
	}

	@Test
	void shouldOmitValidityDateWhenDisabled() {
		QueryStep result = FinalQueryStepComposer.compose(
				queryStep("compiled", "value", true),
				Optional.empty(),
				false,
				DIALECT
		);

		assertFalse(result.getSelects().getValidityDate().isPresent());
	}

	@Test
	void shouldKeepStableEmptyDateProjectionWhenInputHasNoValidityDate() {
		TestDialect dialect = new TestDialect();
		QueryStep result = FinalQueryStepComposer.compose(
				queryStep("compiled", "value", false),
				Optional.empty(),
				true,
				dialect
		);

		ColumnDateRange validityDate = result.getSelects().getValidityDate().orElseThrow();
		assertEquals("null", render(validityDate.getStart()));
		assertEquals("null", render(validityDate.getEnd()));
		assertEquals(1, dialect.getAnyValueCalls());
	}

	@Test
	void shouldNotAddEmptyDateProjectionWhenValidityDateIsDisabled() {
		QueryStep result = FinalQueryStepComposer.compose(
				queryStep("compiled", "value", false),
				Optional.empty(),
				false,
				DIALECT
		);

		assertFalse(result.getSelects().getValidityDate().isPresent());
	}

	@Test
	void shouldJoinAdditionalStepAndIncludeItsExplicitSelects() {
		QueryStep preFinalStep = queryStep("compiled", "value", true);
		QueryStep additionalStep = queryStep("extras", "extra_value", false);

		QueryStep result = FinalQueryStepComposer.compose(
				preFinalStep,
				Optional.of(additionalStep),
				true,
				DIALECT
		);

		assertEquals(List.of(preFinalStep, additionalStep), result.getPredecessors());
		assertEquals(2, result.getSelects().getSqlSelects().size());
		String tableSql = DSL.using(SQLDialect.POSTGRES)
				.select()
				.from(result.getFromTables().getFirst())
				.getSQL()
				.toLowerCase(Locale.ROOT);
		assertTrue(tableSql.contains("from \"compiled\" join \"extras\""));
		assertTrue(tableSql.contains("\"compiled\".\"person_id\" = \"extras\".\"person_id\""));
	}

	private static QueryStep queryStep(String cteName, String valueColumn, boolean withValidityDate) {
		Selects.SelectsBuilder selects = Selects.builder()
				.ids(new SqlIdColumns(field(name("person_id"), String.class)))
				.sqlSelect(new FieldWrapper<>(field(name(valueColumn), Integer.class)));
		if (withValidityDate) {
			selects.validityDate(Optional.of(ColumnDateRange.of(
					field(name("valid_from"), Date.class),
					field(name("valid_to"), Date.class)
			)));
		}
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects.build())
				.build();
	}

	private static String render(Field<?> field) {
		return DSL.using(SQLDialect.POSTGRES).renderInlined(field).toLowerCase(Locale.ROOT);
	}

	private static final class TestDialect implements CompilerDialect {
		private int anyValueCalls;

		int getAnyValueCalls() {
			return anyValueCalls;
		}

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
			anyValueCalls++;
			return DSL.field("any_value({0})", value.getType(), value);
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
