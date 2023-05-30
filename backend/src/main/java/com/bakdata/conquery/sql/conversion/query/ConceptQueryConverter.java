package com.bakdata.conquery.sql.conversion.query;

import java.util.List;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.context.step.QueryStepTransformer;
import org.jooq.Record;
import org.jooq.Select;

public class ConceptQueryConverter implements NodeConverter<ConceptQuery> {

	private final QueryStepTransformer queryStepTransformer;

	public ConceptQueryConverter(QueryStepTransformer queryStepTransformer) {
		this.queryStepTransformer = queryStepTransformer;
	}

	@Override
	public ConversionContext convert(ConceptQuery node, ConversionContext context) {

		ConversionContext contextAfterConversion = context.getNodeConverterService()
														  .convert(node.getRoot(), context);

		QueryStep preFinalStep = contextAfterConversion.getQuerySteps().iterator().next();
		QueryStep finalStep = QueryStep.builder()
									   .cteName(null)  // the final QueryStep won't be converted to a CTE
									   .selects(preFinalStep.getQualifiedSelects())
									   .fromTable(QueryStep.toTableLike(preFinalStep.getCteName()))
									   .conditions(preFinalStep.getConditions())
									   .predecessors(List.of(preFinalStep))
									   .build();

		Select<Record> finalQuery = this.queryStepTransformer.toSelectQuery(finalStep);
		return context.withFinalQuery(finalQuery);
	}

	@Override
	public Class<ConceptQuery> getConversionClass() {
		return ConceptQuery.class;
	}
}
