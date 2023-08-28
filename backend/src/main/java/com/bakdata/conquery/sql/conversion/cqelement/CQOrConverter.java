package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.step.LogicalOperation;
import com.bakdata.conquery.sql.conversion.context.step.StepJoiner;

public class CQOrConverter implements NodeConverter<CQOr> {

	@Override
	public Class<CQOr> getConversionClass() {
		return CQOr.class;
	}

	@Override
	public ConversionContext convert(CQOr orNode, ConversionContext context) {
		if (orNode.getChildren().size() == 1) {
			return context.getNodeConversions().convert(orNode.getChildren().get(0), context);
		}
		return StepJoiner.joinChildren(orNode.getChildren(), context, LogicalOperation.OR);
	}

}
