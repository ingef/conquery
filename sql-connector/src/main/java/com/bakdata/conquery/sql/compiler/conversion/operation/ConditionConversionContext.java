package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.Objects;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;

/** Services available while converting a resolved condition into compiler IR. */
public record ConditionConversionContext(
		CompilerDialect dialect,
		ResolvedConditionConverter conditionConverter
) {

	public ConditionConversionContext {
		Objects.requireNonNull(dialect, "dialect");
		Objects.requireNonNull(conditionConverter, "conditionConverter");
	}
}
