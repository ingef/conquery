package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
class CQTableContext implements Context {


	SqlIdColumns ids;
	Optional<ValidityDate> rawValidityDate;
	Optional<ColumnDateRange> validityDate;
	List<ConnectorSqlSelects> sqlSelects;
	List<SqlFilters> sqlFilters;
	ConnectorSqlTables connectorTables;
	ConversionContext conversionContext;
	@With
	QueryStep previous;

	/**
	 * @return All {@link ConnectorSqlSelects} that are either required for {@link Filter}'s or {@link Select}'s.
	 */
	public List<ConnectorSqlSelects> allSqlSelects() {
		return Stream.concat(sqlSelects.stream(), sqlFilters.stream().map(SqlFilters::getSelects)).toList();
	}

}
