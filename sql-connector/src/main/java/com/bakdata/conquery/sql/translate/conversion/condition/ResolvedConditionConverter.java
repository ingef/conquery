package com.bakdata.conquery.sql.translate.conversion.condition;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.sql.translate.conversion.ConversionDispatcher;
import com.bakdata.conquery.sql.translate.conversion.Converter;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.condition.WhereCondition;
import com.bakdata.conquery.sql.model.operation.ResolvedCondition;

/** Converts resolved condition-model values into translate condition IR. */
public final class ResolvedConditionConverter {

	private final ConversionDispatcher<ResolvedCondition, WhereCondition, ConditionConversionContext> dispatcher;

	public ResolvedConditionConverter() {
		this(List.of());
	}

	/**
	 * Creates a converter with the built-in condition implementations and additional extension converters.
	 *
	 * <p>An extension must support a distinct runtime type; ambiguous converters are rejected by the dispatcher.</p>
	 */
	public ResolvedConditionConverter(
			List<? extends Converter<? extends ResolvedCondition, WhereCondition, ConditionConversionContext>> extensions
	) {
		List<Converter<? extends ResolvedCondition, WhereCondition, ConditionConversionContext>> converters = new ArrayList<>(
				BuiltInConditionConverters.create()
		);
		converters.addAll(extensions);
		this.dispatcher = new ConversionDispatcher<>(converters);
	}

	public WhereCondition convert(ResolvedCondition condition, TranslatorDialect dialect) {
		return dispatcher.convert(condition, new ConditionConversionContext(dialect, this));
	}
}
