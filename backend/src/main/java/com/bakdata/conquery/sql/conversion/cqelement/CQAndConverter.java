package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.model.ConqueryJoinType;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.select.ExistsSqlSelect;

public class CQAndConverter implements NodeConverter<CQAnd> {

	@Override
	public Class<CQAnd> getConversionClass() {
		return CQAnd.class;
	}

	@Override
	public ConversionContext convert(CQAnd andNode, ConversionContext context) {

		QueryStep joined;
		if (andNode.getChildren().size() == 1) {
			ConversionContext withConvertedChild = context.getNodeConversions().convert(andNode.getChildren().get(0), context);
			joined = withConvertedChild.getLastConvertedStep();
		}
		else {
			joined = QueryStepJoiner.joinChildren(
					andNode.getChildren(),
					context,
					ConqueryJoinType.INNER_JOIN,
					andNode.getDateAction()
			);
		}

		if (andNode.getCreateExists().isEmpty()) {
			return context.withQueryStep(joined);
		}

		String joinedNodeName = joined.getCteName();
		ExistsSqlSelect existsSqlSelect = ExistsSqlSelect.withAlias(joinedNodeName);
		return context.withQueryStep(joined.addSqlSelect(existsSqlSelect));
	}

}
