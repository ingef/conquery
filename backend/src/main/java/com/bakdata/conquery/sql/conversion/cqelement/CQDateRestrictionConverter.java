package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;

public class CQDateRestrictionConverter implements NodeConverter<CQDateRestriction> {

	@Override
	public ConversionContext convert(CQDateRestriction node, ConversionContext context) {
		// TODO if there is already a data restriction from a parent node, intersect both date ranges
		ConversionContext childContext = context.withDateRestricionRange(node.getDateRange());
		ConversionContext resultContext = context.getSqlConverterService().convert(node.getChild(), childContext);
		return resultContext.withDateRestricionRange(null);
	}

	@Override
	public Class<CQDateRestriction> getConversionClass() {
		return CQDateRestriction.class;
	}
}
