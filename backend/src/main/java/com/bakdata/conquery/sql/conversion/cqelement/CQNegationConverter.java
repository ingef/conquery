package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQNegation;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.google.common.base.Preconditions;

public class CQNegationConverter implements NodeConverter<CQNegation> {

	@Override
	public Class<CQNegation> getConversionClass() {
		return CQNegation.class;
	}


	@Override
	public ConversionContext convert(CQNegation negationNode, ConversionContext context) {

		ConversionContext converted = context.getNodeConversions()
											 .convert(negationNode.getChild(), context.withNegation(true))
											 .withNegation(false);

		Preconditions.checkArgument(
				converted.getQuerySteps().size() == 1,
				"As we convert only 1 child CQElement, their should be only a single query step."
		);
		QueryStep queryStep = converted.getQuerySteps().get(0);

		if (negationNode.getDateAction() != DateAggregationAction.NEGATE) {
			QueryStep withBlockedValidityDate = queryStep.toBuilder()
														 .selects(queryStep.getSelects().blockValidityDate())
														 .build();
			return context.toBuilder().queryStep(withBlockedValidityDate).build();
		}
		QueryStep withInvertedValidityDate = converted.getSqlDialect()
													  .getDateAggregator()
													  .invertAggregatedIntervals(queryStep, context.getNameGenerator());
		return context.toBuilder().queryStep(withInvertedValidityDate).build();
	}

}
