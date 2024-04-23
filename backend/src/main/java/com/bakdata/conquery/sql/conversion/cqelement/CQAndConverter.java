package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.model.ConqueryJoinType;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;

public class CQAndConverter implements NodeConverter<CQAnd> {

	@Override
	public Class<CQAnd> getConversionClass() {
		return CQAnd.class;
	}

	@Override
	public ConversionContext convert(CQAnd andNode, ConversionContext context) {
		if (andNode.getChildren().size() == 1) {
			return context.getNodeConversions().convert(andNode.getChildren().get(0), context);
		}
		QueryStep joined = QueryStepJoiner.joinChildren(
				andNode.getChildren(),
				context,
				ConqueryJoinType.INNER_JOIN,
				andNode.getDateAction()
		);
		return context.withQueryStep(joined);
	}

}
