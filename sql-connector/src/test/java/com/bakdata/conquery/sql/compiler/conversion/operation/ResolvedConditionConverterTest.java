package com.bakdata.conquery.sql.compiler.conversion.operation;

import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.compiler.conversion.Converter;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.condition.ConditionWrappingWhereCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereCondition;
import com.bakdata.conquery.sql.model.operation.BuiltInConditions;
import com.bakdata.conquery.sql.model.operation.ResolvedCondition;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class ResolvedConditionConverterTest {

	private static final SqlTable TABLE = SqlTable.of("events", "analytics", "events");
	private static final ResolvedColumn COLUMN = new ResolvedColumn("code", TABLE, "event_code", ColumnType.STRING, true);
	private static final CompilerDialect DIALECT = new TestDialect();

	private final ResolvedConditionConverter converter = new ResolvedConditionConverter();

	@Test
	void shouldConvertStringValuesAgainstResolvedColumn() {
		BuiltInConditions.StringValues condition = new BuiltInConditions.StringValues(
				COLUMN,
				new LinkedHashSet<>(List.of("A", "B"))
		);

		assertEquals(
				"\"analytics\".\"events\".\"event_code\" in ('a', 'b')",
				render(converter.convert(condition, DIALECT))
		);
	}

	@Test
	void shouldConvertCompositeConditionsRecursively() {
		BuiltInConditions.AllOf condition = new BuiltInConditions.AllOf(List.of(
				new BuiltInConditions.Presence(COLUMN, true),
				new BuiltInConditions.Not(new BuiltInConditions.Presence(COLUMN, false))
		));

		assertEquals(
				"(\"analytics\".\"events\".\"event_code\" is not null and not (\"analytics\".\"events\".\"event_code\" is null))",
				render(converter.convert(condition, DIALECT))
		);
	}

	@Test
	void shouldDelegateRegexRenderingToDialect() {
		BuiltInConditions.AllOf condition = new BuiltInConditions.AllOf(List.of(
				new BuiltInConditions.Prefixes(COLUMN, List.of("A", "B")),
				new BuiltInConditions.PrefixRange(COLUMN, "A0", "B9")
		));

		assertEquals(
				"((matches(\"analytics\".\"events\".\"event_code\", 'a|b.*')) and (matches(\"analytics\".\"events\".\"event_code\", '[a-b][0-9].*')))",
				render(converter.convert(condition, DIALECT))
		);
	}

	@Test
	void shouldDispatchExtensionCondition() {
		Converter<ExtensionCondition, WhereCondition, ConditionConversionContext> extension = new Converter<>() {
			@Override
			public Class<ExtensionCondition> getConversionClass() {
				return ExtensionCondition.class;
			}

			@Override
			public WhereCondition convert(ExtensionCondition input, ConditionConversionContext context) {
				return new ConditionWrappingWhereCondition(condition("extension_condition"));
			}
		};
		ResolvedConditionConverter converterWithExtension = new ResolvedConditionConverter(List.of(extension));

		assertEquals("(extension_condition)", render(converterWithExtension.convert(new ExtensionCondition(), DIALECT)));
	}

	private static String render(WhereCondition condition) {
		return DSL.using(SQLDialect.POSTGRES).renderInlined(condition.condition()).toLowerCase(Locale.ROOT);
	}

	private record ExtensionCondition() implements ResolvedCondition {
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
		public Condition regexMatches(Field<String> field, String pattern) {
			return condition("matches({0}, {1})", field, inline(pattern));
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
