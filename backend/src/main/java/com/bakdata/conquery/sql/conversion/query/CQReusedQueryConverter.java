package com.bakdata.conquery.sql.conversion.query;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;

public class CQReusedQueryConverter implements NodeConverter<CQReusedQuery> {

	@Override
	public Class<? extends CQReusedQuery> getConversionClass() {
		return CQReusedQuery.class;
	}

	@Override
	public ConversionContext convert(CQReusedQuery reusedQuery, ConversionContext context) {
		CQElement reusableComponents = reusedQuery.getResolvedQuery().getReusableComponents();
		if (!reusedQuery.isExcludeFromSecondaryId()) {
			return context.getNodeConversions().convert(reusableComponents, context);
		}
		ConversionContext withExcludedSecondaryId = context.withSecondaryIdDescription(null);
		return context.getNodeConversions().convert(reusableComponents, withExcludedSecondaryId);
	}

}
