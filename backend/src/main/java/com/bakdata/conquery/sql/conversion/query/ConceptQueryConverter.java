package com.bakdata.conquery.sql.conversion.query;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQNegation;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.sql.compiler.ir.FinalQueryStepComposer;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.rendering.QueryStepRenderer;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.QueryStepComposer;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import lombok.RequiredArgsConstructor;
import org.jooq.Record;
import org.jooq.Select;

import java.util.Optional;

@RequiredArgsConstructor
public class ConceptQueryConverter implements NodeConverter<ConceptQuery> {

	private final QueryStepRenderer queryStepRenderer;

	@Override
	public Class<ConceptQuery> getConversionClass() {
		return ConceptQuery.class;
	}

	@Override
	public ConversionContext convert(ConceptQuery conceptQuery, ConversionContext context) {

		ConversionContext contextAfterConversion = context.getNodeConversions().convert(conceptQuery.getRoot(), context);

		QueryStep preFinalStep = contextAfterConversion.getLastConvertedStep();
		// negation of a single node results in an anti-join with all ids table
		if (preFinalStep.isNegate()) {
			preFinalStep = QueryStepComposer.antiJoinWithAllIdsTable(
					preFinalStep,
					contextAfterConversion,
					CQNegation.determineDateAction(conceptQuery.getDateAggregationMode())
			);
		}

		QueryStep finalStep = FinalQueryStepComposer.compose(
				preFinalStep,
				Optional.ofNullable(contextAfterConversion.getExternalExtras()),
				conceptQuery.getDateAggregationMode() != DateAggregationMode.NONE,
				context.getCompilerDialect()
		);

		Select<Record> finalQuery = this.queryStepRenderer.toSelectQuery(finalStep, context.getCompilerDialect());
		return contextAfterConversion.withFinalQuery(new SqlQuery(finalQuery, conceptQuery.getResultInfos()));
	}
}
