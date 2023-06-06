package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;

public class CQDateRestrictionConverter implements NodeConverter<CQDateRestriction> {

	@Override
	public ConversionContext convert(CQDateRestriction node, ConversionContext context) {
		ConversionContext childContext = context.withDateRestrictionRange(CDateRange.of(node.getDateRange()));
		return context.getNodeConverterService().convert(node.getChild(), childContext).withDateRestrictionRange(null);
	}

	@Override
	public Class<CQDateRestriction> getConversionClass() {
		return CQDateRestriction.class;
	}
}
