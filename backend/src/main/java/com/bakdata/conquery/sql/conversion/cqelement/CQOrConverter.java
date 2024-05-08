package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.model.ConqueryJoinType;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.select.ExistsSqlSelect;

public class CQOrConverter implements NodeConverter<CQOr> {

	@Override
	public Class<CQOr> getConversionClass() {
		return CQOr.class;
	}

	@Override
	public ConversionContext convert(CQOr orNode, ConversionContext context) {

		QueryStep joined;
		if (orNode.getChildren().size() == 1) {
			ConversionContext withConvertedChild = context.getNodeConversions().convert(orNode.getChildren().get(0), context);
			joined = withConvertedChild.getLastConvertedStep();
			context.removeLastConvertedStep(); // we will add the step back after checking if we need an exists select
		}
		else {
			joined = QueryStepJoiner.joinChildren(
					orNode.getChildren(),
					context,
					ConqueryJoinType.OUTER_JOIN,
					orNode.getDateAction()
			);
		}

		if (orNode.getCreateExists().isEmpty()) {
			return context.withQueryStep(joined);
		}

		String joinedNodeName = joined.getCteName();
		ExistsSqlSelect existsSqlSelect = new ExistsSqlSelect(joinedNodeName);
		return context.withQueryStep(joined.addSqlSelect(existsSqlSelect));
	}


}
