package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;

public class SqlConverter {

	private final NodeConversions nodeConversions;

	public SqlConverter(NodeConversions nodeConversions) {
		this.nodeConversions = nodeConversions;
	}

	public SqlQuery convert(QueryDescription queryDescription) {
		ConversionContext converted = nodeConversions.convert(queryDescription);
		return converted.getFinalQuery();
	}
}
