package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;

public class CQOrConverter implements NodeConverter<CQOr> {

	@Override
	public ConversionContext convert(CQOr node, ConversionContext context) {
		if (node.getChildren().size() > 1) {
			throw new IllegalArgumentException("Multiple children are not yet supported");
		}

		return context.getNodeConverterService().convert(node.getChildren().get(0), context);
	}

	@Override
	public Class<CQOr> getConversionClass() {
		return CQOr.class;
	}
}
