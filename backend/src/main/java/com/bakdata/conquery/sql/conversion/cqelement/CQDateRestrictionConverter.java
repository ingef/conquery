package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.sql.conversion.NodeConverter;

public class CQDateRestrictionConverter implements NodeConverter<CQDateRestriction> {

	@Override
	public ConversionContext convert(CQDateRestriction dateRestrictionNode, ConversionContext context) {
		ConversionContext childContext = context.withDateRestrictionRange(CDateRange.of(dateRestrictionNode.getDateRange()));
		return context.getNodeConversions().convert(dateRestrictionNode.getChild(), childContext).withDateRestrictionRange(null);
	}

	@Override
	public Class<CQDateRestriction> getConversionClass() {
		return CQDateRestriction.class;
	}

}
