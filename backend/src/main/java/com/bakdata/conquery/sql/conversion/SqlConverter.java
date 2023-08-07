package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.sql.SqlQuery;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import org.jooq.conf.ParamType;

public class SqlConverter {

	private final NodeConverterService nodeConverterService;

	public SqlConverter(SqlDialect dialect, SqlConnectorConfig config) {
		this.nodeConverterService = new NodeConverterService(dialect, config);
	}

	public SqlQuery convert(QueryDescription queryDescription) {
		ConversionContext converted = nodeConverterService.convert(queryDescription);
		return new SqlQuery(converted.getFinalQuery().getSQL(ParamType.INLINED));
	}
}
