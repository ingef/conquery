package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.List;
import java.util.function.BiFunction;

import com.bakdata.conquery.sql.compiler.conversion.Converter;
import com.bakdata.conquery.sql.compiler.ir.SchemaSql;
import com.bakdata.conquery.sql.compiler.ir.condition.ConditionWrappingWhereCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.StringValuesCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereCondition;
import com.bakdata.conquery.sql.model.operation.BuiltInConditions;
import com.bakdata.conquery.sql.model.operation.ResolvedCondition;
import org.jooq.Field;

/** Built-in mappings from resolved condition values to compiler condition IR. */
final class BuiltInConditionConverters {

	private BuiltInConditionConverters() {
	}

	static List<Converter<? extends ResolvedCondition, WhereCondition, ConditionConversionContext>> create() {
		return List.of(
				converter(BuiltInConditions.AllOf.class, BuiltInConditionConverters::convertAllOf),
				converter(BuiltInConditions.Not.class, BuiltInConditionConverters::convertNot),
				converter(BuiltInConditions.StringValues.class, BuiltInConditionConverters::convertStringValues),
				converter(BuiltInConditions.Presence.class, BuiltInConditionConverters::convertPresence),
				converter(BuiltInConditions.Prefixes.class, BuiltInConditionConverters::convertPrefixes),
				converter(BuiltInConditions.PrefixRange.class, BuiltInConditionConverters::convertPrefixRange)
		);
	}

	private static WhereCondition convertAllOf(
			BuiltInConditions.AllOf condition,
			ConditionConversionContext context
	) {
		return condition.conditions().stream()
				.map(child -> context.conditionConverter().convert(child, context.dialect()))
				.reduce(WhereCondition::and)
				.orElseThrow();
	}

	private static WhereCondition convertNot(
			BuiltInConditions.Not condition,
			ConditionConversionContext context
	) {
		return context.conditionConverter().convert(condition.condition(), context.dialect()).negate();
	}

	private static WhereCondition convertStringValues(
			BuiltInConditions.StringValues condition,
			ConditionConversionContext context
	) {
		Field<String> field = SchemaSql.field(condition.column(), String.class);
		return new StringValuesCondition(field, condition.values().toArray(String[]::new));
	}

	private static WhereCondition convertPresence(
			BuiltInConditions.Presence condition,
			ConditionConversionContext context
	) {
		Field<Object> field = SchemaSql.field(condition.column(), Object.class);
		return new ConditionWrappingWhereCondition(condition.present() ? field.isNotNull() : field.isNull());
	}

	private static WhereCondition convertPrefixes(
			BuiltInConditions.Prefixes condition,
			ConditionConversionContext context
	) {
		String pattern = String.join("|", condition.prefixes()) + context.dialect().regexAnyCharacters();
		Field<String> field = SchemaSql.field(condition.column(), String.class);
		return new ConditionWrappingWhereCondition(context.dialect().regexMatches(field, pattern));
	}

	private static WhereCondition convertPrefixRange(
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
		return new ConditionWrappingWhereCondition(context.dialect().regexMatches(field, pattern.toString()));
	}

	private static <C extends ResolvedCondition> Converter<C, WhereCondition, ConditionConversionContext> converter(
			Class<C> conversionClass,
			BiFunction<C, ConditionConversionContext, WhereCondition> conversion
	) {
		return new Converter<>() {
			@Override
			public Class<C> getConversionClass() {
				return conversionClass;
			}

			@Override
			public WhereCondition convert(C input, ConditionConversionContext context) {
				return conversion.apply(input, context);
			}
		};
	}
}
