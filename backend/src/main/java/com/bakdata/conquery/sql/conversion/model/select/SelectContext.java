package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Optional;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SelectContext<T extends SqlTables> implements Context {

	SqlIdColumns ids;
	Optional<ColumnDateRange> validityDate;
	T tables;
	ConversionContext conversionContext;

	public static SelectContext<ConnectorSqlTables> create(
			SqlIdColumns ids,
			Optional<ColumnDateRange> validityDate,
			ConnectorSqlTables tables,
			ConversionContext conversionContext
	) {
		return new SelectContext<>(ids, validityDate, tables, conversionContext);
	}

	public static SelectContext<ConceptSqlTables> create(
			SqlIdColumns ids,
			Optional<ColumnDateRange> validityDate,
			ConceptSqlTables tables,
			ConversionContext conversionContext
	) {
		return new SelectContext<>(ids, validityDate, tables, conversionContext);
	}

	public SqlFunctionProvider getFunctionProvider() {
		return getSqlDialect().getFunctionProvider();
	}

}
