package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;

public class CQAndConverter implements NodeConverter<CQAnd> {

	@Override
	public ConversionContext convert(CQAnd node, ConversionContext context) {
		if (node.getChildren().size() > 1) {
			throw new IllegalArgumentException("Multiple children are not yet supported");
		}

		return context.getSqlConverterService().convert(node.getChildren().get(0), context);
	}

	@Override
	public Class<CQAnd> getConversionClass() {
		return CQAnd.class;
	}
}
