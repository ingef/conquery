package com.bakdata.conquery.sql.conversion.query;

import java.util.List;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.selects.Selects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.context.step.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;

public class ConceptQueryConverter implements NodeConverter<ConceptQuery> {

	private final QueryStepTransformer queryStepTransformer;

	public ConceptQueryConverter(QueryStepTransformer queryStepTransformer) {
		this.queryStepTransformer = queryStepTransformer;
	}

	@Override
	public Class<ConceptQuery> getConversionClass() {
		return ConceptQuery.class;
	}

	@Override
	public ConversionContext convert(ConceptQuery node, ConversionContext context) {

		ConversionContext contextAfterConversion = context.getNodeConverterService()
														  .convert(node.getRoot(), context);

		QueryStep preFinalStep = contextAfterConversion.getQuerySteps().iterator().next();
		QueryStep finalStep = QueryStep.builder()
									   .cteName(null)  // the final QueryStep won't be converted to a CTE
									   .selects(this.toFinalSelects(preFinalStep, context))
									   .fromTable(QueryStep.toTableLike(preFinalStep.getCteName()))
									   .conditions(preFinalStep.getConditions())
									   .predecessors(List.of(preFinalStep))
									   .build();

		Select<Record> finalQuery = this.queryStepTransformer.toSelectQuery(finalStep);
		return context.withFinalQuery(finalQuery);
	}

	/**
	 * @return The final selects containing the final validity date, if present, as a string aggregation field.
	 */
	private Selects toFinalSelects(QueryStep preFinalStep, ConversionContext context) {

		Selects finalSelects = preFinalStep.getQualifiedSelects();

		if (finalSelects.getValidityDate().isEmpty()) {
			return finalSelects;
		}

		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunction();
		Field<Object> finalValidityDateSelect = functionProvider.daterangeString(finalSelects.getValidityDate().get())
																.as("dates");

		return finalSelects.withValidityDate(ColumnDateRange.of(finalValidityDateSelect));
	}

}
