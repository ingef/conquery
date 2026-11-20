package com.bakdata.conquery.sql.translate.conversion.condition;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.sql.model.operation.BuiltInConditions;
import com.bakdata.conquery.sql.model.operation.ResolvedCondition;
import com.bakdata.conquery.sql.translate.conversion.ConversionDispatcher;
import com.bakdata.conquery.sql.translate.conversion.Converter;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.SchemaSql;
import org.jooq.Condition;
import org.jooq.Field;

/** Converts resolved condition-model values into translate condition IR. */
public final class ResolvedConditionConverter {

	private final ConversionDispatcher<ResolvedCondition, Condition, ConditionConversionContext> dispatcher;

	public ResolvedConditionConverter() {
		this(List.of());
	}

	/**
	 * Creates a converter with the built-in condition implementations and additional extension converters.
	 *
	 * <p>An extension must support a distinct runtime type; ambiguous converters are rejected by the dispatcher.</p>
	 */
	public ResolvedConditionConverter(
			List<? extends Converter<? extends ResolvedCondition, Condition, ConditionConversionContext>> extensions
	) {
		List<Converter<? extends ResolvedCondition, Condition, ConditionConversionContext>> converters = new ArrayList<>(
				BuiltInConditionConverters.create()
		);
		converters.addAll(extensions);
		this.dispatcher = new ConversionDispatcher<>(converters);
	}

	public Condition convert(ResolvedCondition condition, TranslatorDialect dialect) {
		return dispatcher.convert(condition, new ConditionConversionContext(dialect, this));
	}

	Condition convertNegated(ResolvedCondition condition, TranslatorDialect dialect) {
		final Condition converted = convert(condition, dialect);
		ResolvedCondition effectiveCondition = condition;
		while (effectiveCondition instanceof BuiltInConditions.AllOf allOf && allOf.conditions().size() == 1) {
			effectiveCondition = allOf.conditions().getFirst();
		}
		if (effectiveCondition instanceof BuiltInConditions.StringValues values) {
			final Field<String> field = SchemaSql.field(values.column(), String.class);
			return converted.not().or(field.isNull());
		}
		return converted.not();
	}
}
