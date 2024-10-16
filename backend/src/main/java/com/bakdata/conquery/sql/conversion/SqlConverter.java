package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;

public class SqlConverter {

	private final NodeConversions nodeConversions;
	private final ConqueryConfig config;

	public SqlConverter(NodeConversions nodeConversions, ConqueryConfig config) {
		this.nodeConversions = nodeConversions;
		this.config = config;
	}

	public SqlQuery convert(QueryDescription queryDescription, Namespace namespace) {
		ConversionContext converted = nodeConversions.convert(queryDescription, namespace, config);
		return converted.getFinalQuery();
	}
}
