package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;

public class CQOrConverter extends NodeConverter<CQOr> {

	public CQOrConverter() {
		super(CQOr.class);
	}

	@Override
	protected ConversionContext convertNode(CQOr node, ConversionContext context) {
		if (node.getChildren().size() > 1) {
			throw new IllegalArgumentException("Multiple children are not yet supported");
		}

		return context.getSqlConverterService().convertNode(node.getChildren().get(0), context);
	}
}
