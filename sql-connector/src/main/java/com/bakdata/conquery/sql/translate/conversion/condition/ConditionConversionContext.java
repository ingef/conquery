package com.bakdata.conquery.sql.translate.conversion.condition;

import java.util.Objects;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;

/** Services available while converting a resolved condition into translate IR. */
public record ConditionConversionContext(
		TranslatorDialect dialect,
		ResolvedConditionConverter conditionConverter
) {

	public ConditionConversionContext {
		Objects.requireNonNull(dialect, "dialect");
		Objects.requireNonNull(conditionConverter, "conditionConverter");
	}
}
