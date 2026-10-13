package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Locale;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class QueryStepComposerTest {

	private static final CompilerDialect DIALECT = new TestDialect();
	private static final SqlTable ENTITY_TABLE = SqlTable.of("entities", "catalog", "entities");
	private static final EntitySchema ENTITY_SCHEMA = new EntitySchema(
			new ResolvedColumn("person", ENTITY_TABLE, "person_id", ColumnType.STRING, false)
	);

	@Test
	void shouldReturnSinglePositiveStepUnchanged() {
		QueryStep step = queryStep("included", false);

		QueryStep result = compose(List.of(step), JoinMode.INNER, DateAggregationAction.BLOCK);

		assertSame(step, result);
	}

	@Test
	void shouldRejectEmptyInput() {
		assertThrows(
				IllegalArgumentException.class,
				() -> compose(List.of(), JoinMode.INNER, DateAggregationAction.BLOCK)
		);
	}

	@Test
	void shouldJoinPositiveSteps() {
		QueryStep result = compose(
				List.of(queryStep("first", false), queryStep("second", false)),
				JoinMode.INNER,
				DateAggregationAction.BLOCK
		);

		assertEquals("AND-1", result.getCteName());
		assertEquals(2, result.getPredecessors().size());
		assertTrue(render(result).contains("join \"second\""));
	}

	@Test
	void shouldAntiJoinOnlyNegatedStepAgainstEntitySchema() {
		QueryStep excluded = queryStep("excluded", true);

		QueryStep result = compose(List.of(excluded), JoinMode.INNER, DateAggregationAction.BLOCK);

		assertEquals("excluded_negated", result.getCteName());
		assertEquals(List.of(excluded), result.getPredecessors());
		assertTrue(result.getSelects().getValidityDate().isPresent());
		String sql = render(result);
		assertTrue(sql.contains("from \"catalog\".\"entities\" left outer join \"excluded\""));
		assertTrue(sql.contains("\"excluded\".\"person_id\" is null"));
	}

	@Test
	void shouldAntiJoinNegatedSiblingFromPositiveInnerJoin() {
		QueryStep result = compose(
				List.of(queryStep("included", false), queryStep("excluded", true)),
				JoinMode.INNER,
				DateAggregationAction.BLOCK
		);

		assertEquals("AND-1_negated", result.getCteName());
		assertEquals(2, result.getPredecessors().size());
		String sql = render(result);
		assertTrue(sql.contains("from \"included\" left outer join \"excluded\""));
		assertTrue(sql.contains("\"excluded\".\"person_id\" is null"));
	}

	@Test
	void shouldComposePositiveAndNegatedFullOuterJoinWithAllEntities() {
		QueryStep result = compose(
				List.of(queryStep("included", false), queryStep("excluded", true)),
				JoinMode.FULL_OUTER,
				DateAggregationAction.MERGE
		);

		assertEquals("OR-1_negated", result.getCteName());
		assertTrue(result.getSelects().getValidityDate().isPresent());
		String sql = render(result);
		assertTrue(sql.contains("from \"catalog\".\"entities\" left outer join \"excluded\""));
		assertTrue(sql.contains("left outer join \"included\""));
	}

	private static QueryStep compose(
			List<QueryStep> steps,
			JoinMode joinMode,
			DateAggregationAction dateAggregationAction
	) {
		return QueryStepComposer.joinSteps(
				steps,
				joinMode,
				dateAggregationAction,
				ENTITY_SCHEMA,
				DIALECT,
				new SqlNameGenerator(128)
		);
	}

	private static QueryStep queryStep(String cteName, boolean negate) {
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person_id"), String.class)))
						.build())
				.negate(negate)
				.build();
	}

	private static String render(QueryStep queryStep) {
		return DSL.using(SQLDialect.POSTGRES)
				.select()
				.from(queryStep.getFromTables().getFirst())
				.where(queryStep.getConditions())
				.getSQL()
				.toLowerCase(Locale.ROOT);
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
