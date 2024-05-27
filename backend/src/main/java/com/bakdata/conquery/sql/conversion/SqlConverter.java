package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import org.jooq.DSLContext;

public class SqlConverter {

	private final NodeConversions nodeConversions;

	public SqlConverter(SqlDialect dialect, DSLContext dslContext, DatabaseConfig config) {
		this.nodeConversions = new NodeConversions(dialect, dslContext, config);
	}

	public SqlQuery convert(QueryDescription queryDescription) {
		ConversionContext converted = nodeConversions.convert(queryDescription);
		return converted.getFinalQuery();
	}
}
