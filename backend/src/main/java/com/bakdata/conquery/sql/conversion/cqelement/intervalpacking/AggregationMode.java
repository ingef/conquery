package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;

/**
 * Determines if the date that is getting interval-packed is stored as validity date of {@link Selects} or as arbitrary {@link SqlSelect}.
 */
enum AggregationMode {
	VALIDITY_DATE,
	ARBITRARY_SELECT
}
