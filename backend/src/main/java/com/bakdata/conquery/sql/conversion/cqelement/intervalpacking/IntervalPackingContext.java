package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IntervalPackingContext implements Context {

	SqlIdColumns ids;

	/**
	 * The daterange that will be aggregated.
	 */
	ColumnDateRange daterange;

	/**
	 * An optional predecessor of the first interval packing CTE.
	 */
	@Nullable
	QueryStep predecessor;

	SqlTables tables;

	/**
	 * The selects you want to carry through all interval packing steps. They won't get touched besides qualifying.
	 */
	@Builder.Default
	List<SqlSelect> carryThroughSelects = Collections.emptyList();

	@Nullable
	ConversionContext conversionContext;

}
