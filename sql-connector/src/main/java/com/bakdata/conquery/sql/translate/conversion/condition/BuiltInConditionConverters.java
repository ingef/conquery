package com.bakdata.conquery.sql.translate.conversion.condition;

import java.util.List;
import java.util.function.BiFunction;

import com.bakdata.conquery.sql.model.operation.BuiltInConditions;
import com.bakdata.conquery.sql.model.operation.ResolvedCondition;
import com.bakdata.conquery.sql.translate.conversion.Converter;
import com.bakdata.conquery.sql.translate.ir.Conditions;
import com.bakdata.conquery.sql.translate.ir.SchemaSql;
import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.Field;

/** Built-in mappings from resolved condition values to translate condition IR. */
@UtilityClass
class BuiltInConditionConverters {

	static List<Converter<? extends ResolvedCondition, Condition, ConditionConversionContext>> create() {
		return List.of(
				converter(BuiltInConditions.AllOf.class, BuiltInConditionConverters::convertAllOf),
				converter(BuiltInConditions.Not.class, BuiltInConditionConverters::convertNot),
				converter(BuiltInConditions.StringValues.class, BuiltInConditionConverters::convertStringValues),
				converter(BuiltInConditions.Presence.class, BuiltInConditionConverters::convertPresence),
				converter(BuiltInConditions.Prefixes.class, BuiltInConditionConverters::convertPrefixes),
				converter(BuiltInConditions.PrefixRange.class, BuiltInConditionConverters::convertPrefixRange)
		);
	}

	private static Condition convertAllOf(
			BuiltInConditions.AllOf condition,
			ConditionConversionContext context
	) {
		return condition.conditions().stream()
				.map(child -> context.conditionConverter().convert(child, context.dialect()))
				.reduce(Condition::and)
				.orElseThrow();
	}

	private static Condition convertNot(
			BuiltInConditions.Not condition,
			ConditionConversionContext context
	) {
		return context.conditionConverter().convertNegated(condition.condition(), context.dialect());
	}

	private static Condition convertStringValues(
			BuiltInConditions.StringValues condition,
			ConditionConversionContext context
	) {
		Field<String> field = SchemaSql.field(condition.column(), String.class);
		return Conditions.stringValues(field, condition.values().toArray(String[]::new));
	}

	private static Condition convertPresence(
			BuiltInConditions.Presence condition,
			ConditionConversionContext context
	) {
		Field<Object> field = SchemaSql.field(condition.column(), Object.class);
		return condition.present() ? field.isNotNull() : field.isNull();
	}

	private static Condition convertPrefixes(
			BuiltInConditions.Prefixes condition,
			ConditionConversionContext context
	) {
		String pattern = String.join("|", condition.prefixes()) + context.dialect().regexAnyCharacters();
		Field<String> field = SchemaSql.field(condition.column(), String.class);
		return context.dialect().regexMatches(field, pattern);
	}

	private static Condition convertPrefixRange(
			BuiltInConditions.PrefixRange condition,
			ConditionConversionContext context
	) {
		StringBuilder pattern = new StringBuilder();
		for (int index = 0; index < condition.minimum().length(); index++) {
			char minimum = condition.minimum().charAt(index);
			char maximum = condition.maximum().charAt(index);
			if (minimum == maximum) {
				pattern.append(minimum);
			}
			else {
				pattern.append("[%s-%s]".formatted(minimum, maximum));
			}
		}
		pattern.append(context.dialect().regexAnyCharacters());

		Field<String> field = SchemaSql.field(condition.column(), String.class);
		return context.dialect().regexMatches(field, pattern.toString());
	}

	private static <C extends ResolvedCondition> Converter<C, Condition, ConditionConversionContext> converter(
			Class<C> conversionClass,
			BiFunction<C, ConditionConversionContext, Condition> conversion
	) {
		return new Converter<>() {
			@Override
			public Class<C> getConversionClass() {
				return conversionClass;
			}

			@Override
			public Condition convert(C input, ConditionConversionContext context) {
				return conversion.apply(input, context);
			}
		};
	}
}
