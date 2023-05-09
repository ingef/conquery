package com.bakdata.conquery.sql.conversion.query;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;

public class ConceptQueryConverter extends NodeConverter<ConceptQuery> {

	public ConceptQueryConverter() {
		super(ConceptQuery.class);
	}

	@Override
	protected ConversionContext convertNode(ConceptQuery node, ConversionContext context) {
		return context.getSqlConverterService().convertNode(node.getRoot(), context);
	}
}
