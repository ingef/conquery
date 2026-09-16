package com.bakdata.conquery.sql.compiler.ir.concept;

import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.AGGREGATION_FILTER;
import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.INTERVAL_PACKING_SELECTS;
import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.PREPROCESSING;
import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.UNIVERSAL_SELECTS;
import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.UNNEST_DATE;
import static com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingCteStep.INTERVAL_COMPLETE;
import static com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingCteStep.PREVIOUS_END;
import static com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingCteStep.RANGE_INDEX;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class ConceptCtePlannerTest {

	private static final ColumnDateRange DATE_RANGE = ColumnDateRange.of(
			field(name("valid_from"), Date.class),
			field(name("valid_to"), Date.class)
	);

	@Test
	void shouldPlanMandatoryConnectorStepsWithoutIntervalPacking() {
		ConnectorCtePlan plan = planConnector(false, false, false);

		assertFalse(plan.withIntervalPacking());
		assertFalse(plan.excludedFromTimeAggregation());
		assertTrue(plan.tables().isRequiredStep(PREPROCESSING));
		assertTrue(plan.tables().isRequiredStep(AGGREGATION_FILTER));
		assertFalse(plan.tables().isRequiredStep(INTERVAL_COMPLETE));
		assertEquals("events", plan.tables().getPredecessor(PREPROCESSING));
	}

	@Test
	void shouldPlanConnectorIntervalPackingForAggregatedEventDates() {
		ConnectorCtePlan plan = planConnector(true, false, false);

		assertTrue(plan.withIntervalPacking());
		assertFalse(plan.excludedFromTimeAggregation());
		assertTrue(plan.tables().isRequiredStep(PREVIOUS_END));
		assertTrue(plan.tables().isRequiredStep(RANGE_INDEX));
		assertTrue(plan.tables().isRequiredStep(INTERVAL_COMPLETE));
		assertFalse(plan.tables().isRequiredStep(INTERVAL_PACKING_SELECTS));
		assertEquals("connector-preprocessing", plan.tables().getPredecessor(PREVIOUS_END));
	}

	@Test
	void shouldPlanConnectorIntervalSelectsWithoutPropagatingTheirValidityDate() {
		ConnectorCtePlan plan = planConnector(false, true, false);

		assertTrue(plan.withIntervalPacking());
		assertTrue(plan.excludedFromTimeAggregation());
		assertTrue(plan.tables().isRequiredStep(INTERVAL_PACKING_SELECTS));
		assertEquals("connector-interval_complete", plan.tables().getPredecessor(INTERVAL_PACKING_SELECTS));
	}

	@Test
	void shouldPlanUniversalSelectDirectlyOverConceptRoot() {
		SqlTables tables = ConceptCtePlanner.planConcept(
				queryStep(false),
				"concept",
				false,
				new TestDialect(false),
				new SqlNameGenerator(128)
		);

		assertEquals("merged", tables.getPredecessor(UNIVERSAL_SELECTS));
		assertFalse(tables.isRequiredStep(INTERVAL_PACKING_SELECTS));
	}

	@Test
	void shouldPlanSingleColumnConceptIntervalSelectsThroughUnnesting() {
		SqlTables tables = ConceptCtePlanner.planConcept(
				queryStep(true),
				"concept",
				true,
				new TestDialect(true),
				new SqlNameGenerator(128)
		);

		assertEquals("merged", tables.getPredecessor(UNNEST_DATE));
		assertEquals("concept-unnested", tables.getPredecessor(INTERVAL_PACKING_SELECTS));
		assertEquals("concept-interval_packing_selects", tables.getPredecessor(UNIVERSAL_SELECTS));
	}

	@Test
	void shouldRejectConceptIntervalSelectsWithoutValidityDate() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> ConceptCtePlanner.planConcept(
						queryStep(false),
						"concept",
						true,
						new TestDialect(false),
						new SqlNameGenerator(128)
				)
		);

		assertEquals(
				"Can not convert Selects that require interval packing without a validity date present after converting (a) connector(s)",
				exception.getMessage()
		);
	}

	private static ConnectorCtePlan planConnector(boolean aggregateEventDates, boolean eventDateSelectsPresent, boolean singleColumnRanges) {
		return ConceptCtePlanner.planConnector(
				"events",
				"connector",
				aggregateEventDates,
				eventDateSelectsPresent,
				new TestDialect(singleColumnRanges),
				new SqlNameGenerator(128)
		);
	}

	private static QueryStep queryStep(boolean withValidityDate) {
		Selects.SelectsBuilder selects = Selects.builder();
		if (withValidityDate) {
			selects.validityDate(Optional.of(DATE_RANGE));
		}
		return QueryStep.builder()
				.cteName("merged")
				.selects(selects.build())
				.build();
	}

	private record TestDialect(boolean supportsSingleColumnRanges) implements CompilerDialect {

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
