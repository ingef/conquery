package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;

public class CQDateRestrictionConverter extends NodeConverter<CQDateRestriction> {


	public CQDateRestrictionConverter() {
		super(CQDateRestriction.class);
	}

	@Override
	protected ConversionContext convertNode(CQDateRestriction node, ConversionContext context) {
		ConversionContext childContext = context.toBuilder().dateRestricionRange(node.getDateRange()).build();
		ConversionContext resultContext = context.getSqlConverterService().convertNode(node.getChild(), childContext);
		return resultContext.toBuilder().dateRestricionRange(null).build();
	}
}
