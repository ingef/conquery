package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
class CQTableContext implements Context {

	SqlIdColumns ids;
	Optional<ColumnDateRange> validityDate;
	List<SqlSelects> sqlSelects;
	List<SqlFilters> sqlFilters;
	SqlTables connectorTables;
	IntervalPackingContext intervalPackingContext;
	ConversionContext conversionContext;
	@With
	QueryStep previous;

	/**
	 * @return All concepts {@link SqlSelects} that are either required for {@link Filter}'s or {@link Select}'s.
	 */
	public List<SqlSelects> allSqlSelects() {
		return Stream.concat(sqlSelects.stream(), sqlFilters.stream().map(SqlFilters::getSelects)).toList();
	}

	public SqlIdColumns getIds() {
		if (previous == null) {
			return ids;
		}
		return previous.getQualifiedSelects().getIds();
	}

	public Optional<IntervalPackingContext> getIntervalPackingContext() {
		return Optional.ofNullable(intervalPackingContext);
	}

}
