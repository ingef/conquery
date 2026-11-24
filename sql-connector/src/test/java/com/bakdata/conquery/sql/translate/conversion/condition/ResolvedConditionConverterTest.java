package com.bakdata.conquery.sql.translate.conversion.condition;

import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.bakdata.conquery.sql.model.operation.BuiltInConditions;
import com.bakdata.conquery.sql.model.operation.ResolvedCondition;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import com.bakdata.conquery.sql.translate.conversion.Converter;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class ResolvedConditionConverterTest {

	private static final SqlTable TABLE = SqlTable.of("events", "analytics", "events");
	private static final ResolvedColumn COLUMN = new ResolvedColumn("code", TABLE, "event_code", MajorTypeId.STRING, true);
	private static final TranslatorDialect DIALECT = new TestDialect();

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
	void shouldPreserveNullAwareNegationThroughASingletonConjunction() {
		final BuiltInConditions.AllOf allOf = new BuiltInConditions.AllOf(List.of(
				new BuiltInConditions.StringValues(COLUMN, new LinkedHashSet<>(List.of("A")))
		));
		final BuiltInConditions.Not condition = new BuiltInConditions.Not(allOf);
		final Field<String> field = field(name("analytics", "events", "event_code"), String.class);

		assertEquals(render(not(field.eq("A")).or(field.isNull())), render(converter.convert(condition, DIALECT)));
	}

	@Test
	void shouldRejectAmbiguousStringValueConvertersWhenNegated() {
		final Converter<BuiltInConditions.StringValues, Condition, ConditionConversionContext> extension = new Converter<>() {
			@Override
			public Class<BuiltInConditions.StringValues> getConversionClass() {
				return BuiltInConditions.StringValues.class;
			}

			@Override
			public Condition convert(BuiltInConditions.StringValues input, ConditionConversionContext context) {
				return condition("extension_condition");
			}
		};
		final ResolvedConditionConverter converterWithExtension = new ResolvedConditionConverter(List.of(extension));
		final BuiltInConditions.Not condition = new BuiltInConditions.Not(
				new BuiltInConditions.StringValues(COLUMN, new LinkedHashSet<>(List.of("A")))
		);

		assertThrows(IllegalStateException.class, () -> converterWithExtension.convert(condition, DIALECT));
	}

	@Test
	void shouldRejectAmbiguousSingletonConjunctionConvertersWhenNegated() {
		final Converter<BuiltInConditions.AllOf, Condition, ConditionConversionContext> extension = new Converter<>() {
			@Override
			public Class<BuiltInConditions.AllOf> getConversionClass() {
				return BuiltInConditions.AllOf.class;
			}

			@Override
			public Condition convert(BuiltInConditions.AllOf input, ConditionConversionContext context) {
				return condition("extension_condition");
			}
		};
		final ResolvedConditionConverter converterWithExtension = new ResolvedConditionConverter(List.of(extension));
		final BuiltInConditions.AllOf allOf = new BuiltInConditions.AllOf(List.of(
				new BuiltInConditions.StringValues(COLUMN, new LinkedHashSet<>(List.of("A")))
		));

		assertThrows(
				IllegalStateException.class,
				() -> converterWithExtension.convert(new BuiltInConditions.Not(allOf), DIALECT)
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
		final Converter<ExtensionCondition, Condition, ConditionConversionContext> extension = new Converter<>() {
			@Override
			public Class<ExtensionCondition> getConversionClass() {
				return ExtensionCondition.class;
			}

			@Override
			public Condition convert(ExtensionCondition input, ConditionConversionContext context) {
				return condition("extension_condition");
			}
		};
		ResolvedConditionConverter converterWithExtension = new ResolvedConditionConverter(List.of(extension));

		assertEquals("(extension_condition)", render(converterWithExtension.convert(new ExtensionCondition(), DIALECT)));
	}

	private static String render(Condition condition) {
		return DSL.using(SQLDialect.POSTGRES).renderInlined(condition).toLowerCase(Locale.ROOT);
	}

	private record ExtensionCondition() implements ResolvedCondition {
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
