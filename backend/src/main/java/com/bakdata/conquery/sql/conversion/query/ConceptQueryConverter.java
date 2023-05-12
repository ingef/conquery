package com.bakdata.conquery.sql.conversion.query;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;

public class ConceptQueryConverter implements NodeConverter<ConceptQuery> {

	@Override
	public ConversionContext convert(ConceptQuery node, ConversionContext context) {
		return context.getSqlConverterService().convert(node.getRoot(), context);
	}

	@Override
	public Class<ConceptQuery> getConversionClass() {
		return ConceptQuery.class;
	}
}
