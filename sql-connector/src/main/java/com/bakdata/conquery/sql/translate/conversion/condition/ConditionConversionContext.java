package com.bakdata.conquery.sql.translate.conversion.condition;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import lombok.NonNull;

/** Services available while converting a resolved condition into translate IR. */
public record ConditionConversionContext(
		@NonNull TranslatorDialect dialect,
		@NonNull ResolvedConditionConverter conditionConverter
) {
}
