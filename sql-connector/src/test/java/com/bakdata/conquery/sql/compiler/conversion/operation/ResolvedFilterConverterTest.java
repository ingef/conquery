package com.bakdata.conquery.sql.compiler.conversion.operation;

import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.compiler.conversion.Converter;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.condition.ConditionWrappingWhereCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.model.operation.BuiltInFilters;
import com.bakdata.conquery.sql.model.operation.ResolvedFilter;
import com.bakdata.conquery.sql.model.range.NumberRange;
import com.bakdata.conquery.sql.model.range.SubstringRange;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class ResolvedFilterConverterTest {

	private static final SqlTable TABLE = SqlTable.of("events", "analytics", "events");
	private static final ResolvedColumn STRING_COLUMN = new ResolvedColumn(
			"code", TABLE, "event_code", ColumnType.STRING, true
	);
	private static final ResolvedColumn NUMBER_COLUMN = new ResolvedColumn(
			"score", TABLE, "score", ColumnType.DECIMAL, true
	);
	private static final FilterConversionContext CONTEXT = new FilterConversionContext(
			new TestDialect(),
			new SqlNameGenerator(128),
			new SqlTables("events", Map.of(), Map.of()),
			new SqlIdColumns(field(name("analytics", "events", "id"), String.class))
	);

	private final ResolvedFilterConverter converter = new ResolvedFilterConverter();

	@Test
	void shouldConvertStringValuesWithSubstring() {
		BuiltInFilters.StringValues filter = new BuiltInFilters.StringValues(
				"codes",
				STRING_COLUMN,
				new LinkedHashSet<>(List.of("AA", "BB")),
				Optional.of(SubstringRange.between(1, 3))
		);

		SqlFilters result = converter.convert(filter, CONTEXT);

		assertEquals(
				"substring(\"analytics\".\"events\".\"event_code\", 2, 2) in ('aa', 'bb')",
				render(result)
		);
		assertEmptySelects(result.getSelects());
	}

	@Test
	void shouldConvertNumericColumnRange() {
		BuiltInFilters.NumericColumnRange filter = new BuiltInFilters.NumericColumnRange(
				"score",
				NUMBER_COLUMN,
				NumberRange.atLeast(10.5)
		);

		SqlFilters result = converter.convert(filter, CONTEXT);

		assertEquals("\"analytics\".\"events\".\"score\" >= 10.5", render(result));
		assertEmptySelects(result.getSelects());
	}

	@Test
	void shouldDispatchExtensionFilter() {
		Converter<ExtensionFilter, SqlFilters, FilterConversionContext> extension = new Converter<>() {
			@Override
			public Class<ExtensionFilter> getConversionClass() {
				return ExtensionFilter.class;
			}

			@Override
			public SqlFilters convert(ExtensionFilter input, FilterConversionContext context) {
				return new SqlFilters(
						ConnectorSqlSelects.none(),
						WhereClauses.builder()
								.eventFilter(new ConditionWrappingWhereCondition(condition("extension_filter")))
								.build()
				);
			}
		};
		ResolvedFilterConverter converterWithExtension = new ResolvedFilterConverter(List.of(extension));

		assertEquals("(extension_filter)", render(converterWithExtension.convert(new ExtensionFilter("extension"), CONTEXT)));
	}

	private static String render(SqlFilters filters) {
		assertEquals(1, filters.getWhereClauses().getEventFilters().size());
		assertTrue(filters.getWhereClauses().getPreprocessingConditions().isEmpty());
		assertTrue(filters.getWhereClauses().getGroupFilters().isEmpty());
		return DSL.using(SQLDialect.POSTGRES)
				.renderInlined(filters.getWhereClauses().getEventFilters().getFirst().condition())
				.toLowerCase(Locale.ROOT);
	}

	private static void assertEmptySelects(ConnectorSqlSelects selects) {
		assertTrue(selects.getPreprocessingSelects().isEmpty());
		assertTrue(selects.getAggregationSelects().isEmpty());
		assertTrue(selects.getEventDateSelects().isEmpty());
		assertTrue(selects.getFinalSelects().isEmpty());
		assertTrue(selects.getAdditionalPredecessor().isEmpty());
	}

	private record ExtensionFilter(String name) implements ResolvedFilter {
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
