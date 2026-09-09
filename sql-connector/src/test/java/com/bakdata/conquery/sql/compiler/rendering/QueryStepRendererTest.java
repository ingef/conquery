package com.bakdata.conquery.sql.compiler.rendering;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Locale;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.ProjectionMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class QueryStepRendererTest {

	private static final CompilerDialect DIALECT = new TestDialect();

	@Test
	void shouldRenderPredecessorAsCommonTableExpression() {
		QueryStep predecessor = queryStep("source_step", "events");
		QueryStep resultStep = QueryStep.builder()
				.selects(predecessor.getQualifiedSelects())
				.fromTable(QueryStep.toTableLike("source_step"))
				.predecessor(predecessor)
				.projectionMode(ProjectionMode.AGGREGATED)
				.build();

		String sql = render(resultStep);

		assertTrue(sql.contains("with \"source_step\" as"));
		assertTrue(sql.contains("from \"events\""));
		assertTrue(sql.contains("from \"source_step\""));
	}

	@Test
	void shouldRenderUnionAll() {
		QueryStep first = queryStep(null, "first_events").toBuilder()
				.projectionMode(ProjectionMode.INDIVIDUAL)
				.build();
		QueryStep second = queryStep(null, "second_events").toBuilder()
				.projectionMode(ProjectionMode.INDIVIDUAL)
				.build();
		QueryStep union = QueryStep.createUnionAllStep(List.of(first, second), null, List.of(), false);

		String sql = render(union);

		assertTrue(sql.contains("from \"first_events\""));
		assertTrue(sql.contains("union all"));
		assertTrue(sql.contains("from \"second_events\""));
	}

	private static QueryStep queryStep(String cteName, String tableName) {
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person"), String.class)))
						.build())
				.fromTable(table(name(tableName)))
				.build();
	}

	private static String render(QueryStep queryStep) {
		return new QueryStepRenderer(DSL.using(SQLDialect.POSTGRES))
				.toSelectQuery(queryStep, DIALECT)
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
		public <T> Field<T> anyValue(Field<T> field) {
			return field;
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
