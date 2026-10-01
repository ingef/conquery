package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;

public class SqlConverter {

	private final NodeConversions nodeConversions;
	private final ConqueryConfig config;
	private final ResolvedQueryAdapter resolvedQueryAdapter;
	private final LegacyCompilerDialect dialect;

	public SqlConverter(
			NodeConversions nodeConversions,
			ConqueryConfig config,
			ResolvedQueryAdapter resolvedQueryAdapter,
			LegacyCompilerDialect dialect
	) {
		this.nodeConversions = nodeConversions;
		this.config = config;
		this.resolvedQueryAdapter = resolvedQueryAdapter;
		this.dialect = dialect;
	}

	public SqlQuery convert(QueryDescription queryDescription) {
		if (queryDescription instanceof ConceptQuery conceptQuery) {
			return resolvedQueryAdapter.compile(conceptQuery, dialect);
		}
		if (queryDescription instanceof SecondaryIdQuery secondaryIdQuery) {
			return resolvedQueryAdapter.compile(secondaryIdQuery, dialect);
		}
		ConversionContext converted = nodeConversions.convert(queryDescription, config);
		return converted.getFinalQuery();
	}
}
