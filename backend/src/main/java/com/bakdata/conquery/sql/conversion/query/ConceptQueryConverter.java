package com.bakdata.conquery.sql.conversion.query;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;

@RequiredArgsConstructor
public class ConceptQueryConverter implements NodeConverter<ConceptQuery> {

	private final QueryStepTransformer queryStepTransformer;

	@Override
	public Class<ConceptQuery> getConversionClass() {
		return ConceptQuery.class;
	}

	@Override
	public ConversionContext convert(ConceptQuery conceptQuery, ConversionContext context) {

		ConversionContext contextAfterConversion = context.getNodeConversions().convert(conceptQuery.getRoot(), context);

		QueryStep preFinalStep = contextAfterConversion.getQuerySteps().iterator().next();
		Selects preFinalSelects = preFinalStep.getQualifiedSelects();

		QueryStep finalStep = QueryStep.builder()
									   .cteName(null)  // the final QueryStep won't be converted to a CTE
									   .selects(getFinalSelects(conceptQuery, preFinalSelects, context.getSqlDialect().getFunctionProvider()))
									   .fromTable(QueryStep.toTableLike(preFinalStep.getCteName()))
									   .groupBy(getFinalGroupBySelects(preFinalSelects))
									   .predecessors(List.of(preFinalStep))
									   .build();

		Select<Record> finalQuery = this.queryStepTransformer.toSelectQuery(finalStep);
		return contextAfterConversion.withFinalQuery(new SqlQuery(finalQuery, conceptQuery.getResultInfos()));
	}

	private Selects getFinalSelects(ConceptQuery conceptQuery, Selects preFinalSelects, SqlFunctionProvider functionProvider) {
		if (conceptQuery.getDateAggregationMode() == DateAggregationMode.NONE) {
			return preFinalSelects.blockValidityDate();
		}
		else if (preFinalSelects.getValidityDate().isEmpty()) {
			return preFinalSelects.withValidityDate(ColumnDateRange.empty());
		}
		Field<String> validityDateStringAggregation = functionProvider.daterangeStringAggregation(preFinalSelects.getValidityDate().get());
		return preFinalSelects.withValidityDate(ColumnDateRange.of(validityDateStringAggregation).as(SharedAliases.DATES_COLUMN.getAlias()));
	}

	private List<Field<?>> getFinalGroupBySelects(Selects preFinalSelects) {
		List<Field<?>> groupBySelects = new ArrayList<>();
		groupBySelects.addAll(preFinalSelects.getIds().toFields());
		groupBySelects.addAll(preFinalSelects.explicitSelects());
		return groupBySelects;
	}

}
