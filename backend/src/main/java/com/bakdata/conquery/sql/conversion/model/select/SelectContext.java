package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Optional;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Value;
import org.jooq.Field;

@Value
public class SelectContext implements Context {

	Field<Object> primaryColumn;
	Optional<ColumnDateRange> validityDate;
	SqlTables<ConnectorCteStep> connectorTables;
	ConversionContext parentContext;

	public static SelectContext forUniversalSelects(Field<Object> primaryColumn, Optional<ColumnDateRange> validityDate, ConversionContext conversionContext) {
		return new SelectContext(primaryColumn, validityDate, null, conversionContext);
	}

	public static SelectContext forConnectorSelects(
			Field<Object> primaryColumn,
			Optional<ColumnDateRange> validityDate,
			SqlTables<ConnectorCteStep> connectorTables,
			ConversionContext conversionContext
	) {
		return new SelectContext(primaryColumn, validityDate, connectorTables, conversionContext);
	}

	@Override
	public NameGenerator getNameGenerator() {
		return this.parentContext.getNameGenerator();
	}

}
