package com.bakdata.conquery.sql.query;

import java.util.Objects;

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

	record ListType(Primitive elementType) implements ResultType {
		public ListType {
			Objects.requireNonNull(elementType, "elementType");
		}
	}
}
