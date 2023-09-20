package com.bakdata.conquery.sql.conversion.query;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.model.Selects;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;

public class ConceptQueryConverter implements NodeConverter<ConceptQuery> {

	public static final String FINAL_VALIDITY_DATE_COLUMN_NAME = "dates";
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

		ConversionContext contextAfterConversion = context.getNodeConversions()
														  .convert(node.getRoot(), context);


		QueryStep previousStep = contextAfterConversion.getQuerySteps().iterator().next();
		Selects finalSelects = this.toFinalSelects(previousStep, context);
		QueryStep finalStep = QueryStep.builder()
									   .cteName(null)  // the final QueryStep won't be converted to a CTE
									   .selects(finalSelects)
									   .fromTable(QueryStep.toTableLike(previousStep.getCteName()))
									   .conditions(Collections.emptyList())
									   .predecessors(List.of(previousStep))
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
		Field<Object> finalValidityDateSelect = functionProvider.validityDateStringAggregation(finalSelects.getValidityDate().get())
																.as(FINAL_VALIDITY_DATE_COLUMN_NAME);

		return finalSelects.withValidityDate(ColumnDateRange.of(finalValidityDateSelect));
	}

}
