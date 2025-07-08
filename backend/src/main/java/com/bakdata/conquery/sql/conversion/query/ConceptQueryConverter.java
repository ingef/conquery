package com.bakdata.conquery.sql.conversion.query;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.ConqueryJoinType;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.TableLike;

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

		QueryStep preFinalStep = contextAfterConversion.getLastConvertedStep();
		Selects preFinalSelects = getPreFinalSelects(preFinalStep, contextAfterConversion);
		List<QueryStep> predecessors = Stream.concat(Stream.of(preFinalStep), Stream.ofNullable(contextAfterConversion.getExternalExtras())).toList();

		QueryStep finalStep = QueryStep.builder()
									   .cteName(null)  // the final QueryStep won't be converted to a CTE
									   .selects(getFinalSelects(conceptQuery, preFinalSelects, context.getSqlDialect().getFunctionProvider()).toFinalRepresentation())
									   .fromTable(getFinalTable(preFinalStep, contextAfterConversion))
									   .groupBy(getFinalGroupBySelects(preFinalSelects))
									   .predecessors(predecessors)
									   .build();

		Select<Record> finalQuery = this.queryStepTransformer.toSelectQuery(finalStep);
		return contextAfterConversion.withFinalQuery(new SqlQuery(finalQuery, conceptQuery.getResultInfos()));
	}

	private static Selects getPreFinalSelects(QueryStep preFinalStep, ConversionContext context) {
		Selects preFinalStepSelects = preFinalStep.getQualifiedSelects();
		QueryStep externalExtras = context.getExternalExtras();
		if (externalExtras == null) {
			return preFinalStepSelects;
		}
		// adding extra selects
		List<SqlSelect> concatenated = Stream.concat(
				preFinalStepSelects.getSqlSelects().stream(),
				externalExtras.getQualifiedSelects().getSqlSelects().stream()
		).toList();
		return preFinalStepSelects.toBuilder().sqlSelects(concatenated).build();
	}

	private static TableLike<Record> getFinalTable(QueryStep preFinalStep, ConversionContext context) {
		QueryStep externalExtras = context.getExternalExtras();
		if (externalExtras == null) {
			return QueryStep.toTableLike(preFinalStep.getCteName());
		}
		return QueryStepJoiner.constructJoinedTable(
				List.of(preFinalStep, externalExtras),
				ConqueryJoinType.INNER_JOIN,
				context
		);
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
