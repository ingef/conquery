package com.bakdata.conquery.sql.compiler.ir.interval;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.Builder;
import lombok.Value;

/** Inputs required to transform a date range into interval-packing query steps. */
@Value
@Builder
public class IntervalPackingContext {

	SqlIdColumns ids;

	/**
	 * The daterange that will be aggregated.
	 */
	ColumnDateRange daterange;

	/**
	 * An optional predecessor of the first interval packing CTE.
	 */
	@Builder.Default
	Optional<QueryStep> predecessor = Optional.empty();

	SqlTables tables;

	/**
	 * The selects you want to carry through all interval packing steps. They won't get touched besides qualifying.
	 */
	@Builder.Default
	List<SqlSelect> carryThroughSelects = Collections.emptyList();

}
