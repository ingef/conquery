package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.model.QueryStepComposer;

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
			joined = QueryStepComposer.joinChildren(
					andNode.getChildren(),
					context,
					JoinMode.INNER,
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
