package com.bakdata.conquery.sql.conversion;

import java.util.Optional;

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
	private final ResolvedQueryMapper resolvedQueryMapper;
	private final ResolvedQueryAdapter resolvedQueryAdapter;
	private final LegacyCompilerDialect dialect;

	public SqlConverter(
			NodeConversions nodeConversions,
			ConqueryConfig config,
			ResolvedQueryMapper resolvedQueryMapper,
			ResolvedQueryAdapter resolvedQueryAdapter,
			LegacyCompilerDialect dialect
	) {
		this.nodeConversions = nodeConversions;
		this.config = config;
		this.resolvedQueryMapper = resolvedQueryMapper;
		this.resolvedQueryAdapter = resolvedQueryAdapter;
		this.dialect = dialect;
	}

	public SqlQuery convert(QueryDescription queryDescription) {
		if (queryDescription instanceof ConceptQuery conceptQuery) {
			return resolvedQueryAdapter.compile(
					resolvedQueryMapper.map(conceptQuery, Optional.empty()), dialect, conceptQuery.getResultInfos());
		}
		if (queryDescription instanceof SecondaryIdQuery secondaryIdQuery) {
			return resolvedQueryAdapter.compile(
					resolvedQueryMapper.map(
							secondaryIdQuery.getQuery(),
							Optional.of(secondaryIdQuery.getSecondaryId().resolve()),
							secondaryIdQuery.getResultInfos()),
					dialect,
					secondaryIdQuery.getResultInfos()
			);
		}
		ConversionContext converted = nodeConversions.convert(queryDescription, config);
		return converted.getFinalQuery();
	}
}
