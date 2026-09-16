package com.bakdata.conquery.sql.model.result;

import jakarta.validation.constraints.NotNull;

/** Logical type of a result column. */
public sealed interface ResultType permits ResultType.Primitive, ResultType.ListType {

	enum Primitive implements ResultType {
		BOOLEAN,
		INTEGER,
		NUMERIC,
		DATE,
		DATE_RANGE,
		STRING,
		MONEY
	}

	record ListType(@NotNull Primitive elementType) implements ResultType {
	}
}
