package com.bakdata.conquery.sql.execution;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.dialect.pg.PostgreSqlFunctionProvider;
import com.bakdata.conquery.util.DateReader;

public class PgSqlCDateSetParser extends DefaultCDateSetParser {
	private static final String DATE_RANGE_SEP = ",";

	private static final String DATE_RANGE_END_EXCLUSIVE = ")";
	private static final String DATE_RANGE_END_INCLUSIVE = "]";
	private static final String DATE_RANGE_BEGIN_INCLUSIVE = "[";

	private static final String DATE_SET_START = "{";
	private static final String DATE_SET_END = "}";
	private static final String EMPTY_DATE_SET = "empty";

	public PgSqlCDateSetParser() {
		super(
				new DateReader(Set.of("yyyy-MM-dd"), Collections.emptyList(), Collections.emptyList()),
				DATE_RANGE_SEP,
				DATE_RANGE_SEP,
				PostgreSqlFunctionProvider.NEGATIVE_INFINITY_DATE_VALUE,
				PostgreSqlFunctionProvider.INFINITY_DATE_VALUE,
				DATE_RANGE_END_EXCLUSIVE,
				DATE_RANGE_END_INCLUSIVE,
				DATE_RANGE_BEGIN_INCLUSIVE,
				DATE_SET_START,
				DATE_SET_END,
				EMPTY_DATE_SET
		);
	}

}
