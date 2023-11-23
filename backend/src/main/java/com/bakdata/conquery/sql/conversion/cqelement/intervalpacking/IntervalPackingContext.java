package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Builder;
import lombok.Value;
import org.jooq.Field;

@Value
@Builder
public class IntervalPackingContext implements Context {

	/**
	 * A unique CTE label which will be suffixed with the interval packing CTE names.
	 */
	String nodeLabel;

	Field<Object> primaryColumn;

	ColumnDateRange validityDate;

	/**
	 * An optional predecessor of the first interval packing CTE.
	 */
	QueryStep predecessor;

	SqlTables<IntervalPackingCteStep> intervalPackingTables;

	/**
	 * The selects you want to carry through all interval packing steps. They won't get touched besides qualifying.
	 */
	@Builder.Default
	List<SqlSelect> carryThroughSelects = Collections.emptyList();

	@CheckForNull
	public QueryStep getPredecessor() {
		return predecessor;
	}

}
