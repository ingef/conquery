package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.model.QueryStepComposer;

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
		}
		else {
			joined = QueryStepComposer.joinChildren(
					orNode.getChildren(),
					context,
					JoinMode.FULL_OUTER,
					orNode.getDateAction()
			);
		}

		if (orNode.getCreateExists().isEmpty()) {
			return context.withQueryStep(joined);
		}

		String joinedNodeName = joined.getCteName();
		ExistsSqlSelect existsSqlSelect = ExistsSqlSelect.withAlias(joinedNodeName);
		return context.withQueryStep(joined.addSqlSelect(existsSqlSelect));
	}


}
