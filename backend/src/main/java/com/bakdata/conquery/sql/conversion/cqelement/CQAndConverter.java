package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.step.LogicalOperation;
import com.bakdata.conquery.sql.conversion.context.step.StepJoiner;

public class CQAndConverter implements NodeConverter<CQAnd> {

	@Override
	public Class<CQAnd> getConversionClass() {
		return CQAnd.class;
	}

	@Override
	public ConversionContext convert(CQAnd andNode, ConversionContext context) {
		if (andNode.getChildren().size() == 1) {
			return context.getNodeConverterService().convert(andNode.getChildren().get(0), context);
		}
		return StepJoiner.joinChildren(andNode.getChildren(), context, LogicalOperation.AND);
	}

}
