package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
class CQTableContext implements Context {

	String connector;
	SqlIdColumns ids;
	/**
	 * Unaliased validity-date expression for predicates rendered in the preprocessing SELECT.
	 */
	ColumnDateRange rawValidityDate;
	List<ConnectorSqlSelects> sqlSelects;
	List<SqlFilters> sqlFilters;
	ConnectorSqlTables connectorTables;
	ConversionContext conversionContext;
	@With
	QueryStep previous;


	public ColumnDateRange getValidityDate() {
		return rawValidityDate.asValidityDateRange(connectorTables.getName());
	}

	/**
	 * @return All {@link ConnectorSqlSelects} that are either required for {@link Filter}'s or {@link Select}'s.
	 */
	public List<ConnectorSqlSelects> allSqlSelects() {
		return Stream.concat(sqlSelects.stream(), sqlFilters.stream().map(SqlFilters::getSelects)).toList();
	}

	@Override
	public LegacyCompilerDialect getCompilerDialect() {
		return getConversionContext().getCompilerDialect();
	}
}
