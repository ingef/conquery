package com.bakdata.conquery.sql.conversion.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.sql.ConceptSqlQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.model.Selects;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.impl.DSL;

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
	public ConversionContext convert(ConceptQuery conceptQuery, ConversionContext context) {

		ConversionContext contextAfterConversion = context.getNodeConversions()
														  .convert(conceptQuery.getRoot(), context);

		QueryStep preFinalStep = contextAfterConversion.getQuerySteps().iterator().next();
		Selects preFinalSelects = preFinalStep.getQualifiedSelects();

		QueryStep finalStep = QueryStep.builder()
									   .cteName(null)  // the final QueryStep won't be converted to a CTE
									   .selects(getFinalSelects(conceptQuery, preFinalSelects, context.getSqlDialect().getFunctionProvider()))
									   .fromTable(QueryStep.toTableLike(preFinalStep.getCteName()))
									   .groupBy(getFinalGroupBySelects(preFinalSelects, context.getSqlDialect()))
									   .predecessors(List.of(preFinalStep))
									   .build();

		Select<Record> finalQuery = this.queryStepTransformer.toSelectQuery(finalStep);
		return context.withFinalQuery(new ConceptSqlQuery(finalQuery, conceptQuery.getResultInfos()));
	}

	private Selects getFinalSelects(ConceptQuery conceptQuery, Selects preFinalSelects, SqlFunctionProvider functionProvider) {
		if (conceptQuery.getDateAggregationMode() == DateAggregationMode.NONE) {
			return preFinalSelects.blockValidityDate();
		}
		else if (preFinalSelects.getValidityDate().isEmpty()) {
			Field<String> emptyRange = DSL.field(DSL.val("{}"));
			return preFinalSelects.withValidityDate(ColumnDateRange.of(emptyRange));
		}
		Field<String> validityDateStringAggregation = functionProvider.validityDateStringAggregation(preFinalSelects.getValidityDate().get())
																	  .as(FINAL_VALIDITY_DATE_COLUMN_NAME);
		return preFinalSelects.withValidityDate(ColumnDateRange.of(validityDateStringAggregation));
	}

	private List<Field<?>> getFinalGroupBySelects(Selects preFinalSelects, SqlDialect sqlDialect) {
		if (!sqlDialect.requiresAggregationInFinalStep()) {
			return Collections.emptyList();
		}
		List<Field<?>> groupBySelects = new ArrayList<>();
		groupBySelects.add(preFinalSelects.getPrimaryColumn());
		groupBySelects.addAll(preFinalSelects.explicitSelects());
		return groupBySelects;
	}

}
