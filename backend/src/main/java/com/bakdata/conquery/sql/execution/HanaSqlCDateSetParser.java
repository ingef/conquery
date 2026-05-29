package com.bakdata.conquery.sql.execution;

import static com.bakdata.conquery.sql.conversion.dialect.hana.HanaSqlFunctionProvider.DATE_SET_SEPARATOR;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.dialect.hana.HanaSqlFunctionProvider;
import com.bakdata.conquery.util.DateReader;

public class HanaSqlCDateSetParser extends DefaultCDateSetParser {
	private static final String DATE_RANGE_SEP = "/";

	private static final String DATE_RANGE_END_EXCLUSIVE = ")";
	private static final String DATE_RANGE_END_INCLUSIVE = "]";
	private static final String DATE_RANGE_BEGIN_INCLUSIVE = "[";

	private static final String DATE_SET_START = "{";
	private static final String DATE_SET_END = "}";
	private static final String EMPTY_DATE_SET = "{}";

	public HanaSqlCDateSetParser() {
		super(
				new DateReader(Set.of("yyyy-M-dd"), Collections.emptyList(), Collections.emptyList()),
				Character.toString(DATE_SET_SEPARATOR),
				DATE_RANGE_SEP,
				HanaSqlFunctionProvider.MIN_DATE_VALUE,
				HanaSqlFunctionProvider.MAX_DATE_VALUE,
				DATE_RANGE_END_EXCLUSIVE,
				DATE_RANGE_END_INCLUSIVE,
				DATE_RANGE_BEGIN_INCLUSIVE,
				DATE_SET_START,
				DATE_SET_END,
				EMPTY_DATE_SET
		);
	}

}
