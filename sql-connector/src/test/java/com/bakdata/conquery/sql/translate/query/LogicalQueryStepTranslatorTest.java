package com.bakdata.conquery.sql.translate.query;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.JoinMode;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class LogicalQueryStepTranslatorTest {

	private static final TranslatorDialect DIALECT = new TestDialect();
	private static final SqlTable ENTITY_TABLE = SqlTable.of("entities", "catalog", "entities");
	private static final EntitySchema ENTITY_SCHEMA = new EntitySchema(
			new ResolvedColumn("person", ENTITY_TABLE, "person_id", ColumnType.STRING, false)
	);

	@Test
	void shouldReturnSingleChildWithoutExistsProjection() {
		QueryStep child = queryStep("child");

		QueryStep compiled = translate(List.of(child), JoinMode.INNER, false);

		assertSame(child, compiled);
	}

	@Test
	void shouldAddExistsProjectionForSingleChild() {
		QueryStep compiled = translate(List.of(queryStep("child")), JoinMode.INNER, true);

		assertEquals(1, compiled.getSelects().getSqlSelects().size());
		assertEquals("child", compiled.getSelects().explicitSelects().getFirst().getName());
	}

	@Test
	void shouldComposeDisjunctionBeforeAddingExistsProjection() {
		QueryStep compiled = translate(
				List.of(queryStep("first"), queryStep("second")),
				JoinMode.FULL_OUTER,
				true
		);

		assertEquals("OR-1", compiled.getCteName());
		assertEquals(2, compiled.getPredecessors().size());
		assertEquals("OR-1", compiled.getSelects().explicitSelects().getFirst().getName());
	}

	private static QueryStep translate(List<QueryStep> children, JoinMode joinMode, boolean createExists) {
		return LogicalQueryStepTranslator.translate(
				children,
				joinMode,
				DateAggregationAction.BLOCK,
				createExists,
				ENTITY_SCHEMA,
				DIALECT,
				new SqlNameGenerator(128)
		);
	}

	private static QueryStep queryStep(String cteName) {
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person_id"), String.class)))
						.build())
				.build();
	}

	private static final class TestDialect implements TranslatorDialect {

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
