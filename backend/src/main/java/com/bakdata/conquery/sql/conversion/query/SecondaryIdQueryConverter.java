package com.bakdata.conquery.sql.conversion.query;

import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.google.common.base.Preconditions;

public class SecondaryIdQueryConverter implements NodeConverter<SecondaryIdQuery> {

	@Override
	public Class<? extends SecondaryIdQuery> getConversionClass() {
		return SecondaryIdQuery.class;
	}

	@Override
	public ConversionContext convert(SecondaryIdQuery query, ConversionContext context) {

		ConversionContext withConvertedQuery = context.getNodeConversions().convert(
				query.getQuery(),
				context.withSecondaryIdDescription(query.getSecondaryId())
		);

		Preconditions.checkArgument(withConvertedQuery.getFinalQuery() != null, "The SecondaryIdQuery's query should be converted by now.");
		SecondaryIdSqlQuery secondaryIdSqlQuery = SecondaryIdSqlQuery.overwriteResultInfos(withConvertedQuery.getFinalQuery(), query.getResultInfos());

		return withConvertedQuery.withFinalQuery(secondaryIdSqlQuery);
	}

}
