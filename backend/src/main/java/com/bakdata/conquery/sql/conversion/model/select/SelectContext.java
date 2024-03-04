package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Optional;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Value;

@Value
public class SelectContext implements Context {

	SqlIdColumns ids;
	Optional<ColumnDateRange> validityDate;
	SqlTables connectorTables;
	ConversionContext parentContext;

	public static SelectContext forUniversalSelects(SqlIdColumns ids, Optional<ColumnDateRange> validityDate, ConversionContext conversionContext) {
		return new SelectContext(ids, validityDate, null, conversionContext);
	}

	public static SelectContext forConnectorSelects(
			SqlIdColumns ids,
			Optional<ColumnDateRange> validityDate,
			SqlTables connectorTables,
			ConversionContext conversionContext
	) {
		return new SelectContext(ids, validityDate, connectorTables, conversionContext);
	}

	@Override
	public NameGenerator getNameGenerator() {
		return this.parentContext.getNameGenerator();
	}

}
