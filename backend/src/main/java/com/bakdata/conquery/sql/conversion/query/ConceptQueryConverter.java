package com.bakdata.conquery.sql.conversion.query;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQNegation;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.*;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.Record;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ConceptQueryConverter implements NodeConverter<ConceptQuery> {

	private final QueryStepTransformer queryStepTransformer;

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

	@Override
	public Class<ConceptQuery> getConversionClass() {
		return ConceptQuery.class;
	}

	@Override
	public ConversionContext convert(ConceptQuery conceptQuery, ConversionContext context) {

		SqlFunctionProvider functionProvider = context.getCompilerDialect().getFunctionProvider();
		ConversionContext contextAfterConversion = context.getNodeConversions().convert(conceptQuery.getRoot(), context);

		QueryStep preFinalStep = contextAfterConversion.getLastConvertedStep();
		// negation of a single node results in an anti-join with all ids table
		if (preFinalStep.isNegate()) {
			preFinalStep = QueryStepJoiner.antiJoinWithAllIdsTable(preFinalStep, contextAfterConversion, CQNegation.determineDateAction(conceptQuery.getDateAggregationMode()));
		}

		Selects preFinalSelects = getPreFinalSelects(preFinalStep, contextAfterConversion);
		List<QueryStep> predecessors = Stream.concat(Stream.of(preFinalStep), Stream.ofNullable(contextAfterConversion.getExternalExtras())).toList();

		QueryStep finalStep = QueryStep.builder()
				.cteName(null)  // the final QueryStep won't be converted to a CTE
				.selects(getFinalSelects(conceptQuery, preFinalSelects, context.getCompilerDialect()))
				.fromTable(getFinalTable(preFinalStep, contextAfterConversion))
				.groupBy(getFinalGroupBySelects(preFinalSelects))
				.predecessors(predecessors)
				.build();

		Select<Record> finalQuery = this.queryStepTransformer.toSelectQuery(finalStep, functionProvider);
		return contextAfterConversion.withFinalQuery(new SqlQuery(finalQuery, conceptQuery.getResultInfos()));
	}

	private Selects getFinalSelects(ConceptQuery conceptQuery, Selects preFinalSelects, LegacyCompilerDialect dialect) {
		SqlFunctionProvider functionProvider = dialect.getFunctionProvider();
		Selects finalSelects = preFinalSelects;
		if (conceptQuery.getDateAggregationMode() == DateAggregationMode.NONE) {
			finalSelects = preFinalSelects.blockValidityDate();
		}

// In case all final selects have no validity-date, we convert it to infinity.
		if (preFinalSelects.getValidityDate().isEmpty()) {
			return preFinalSelects.toBuilder()
					.validityDate(Optional.of(functionProvider.emptyColumnDateRange()))
					.build();
		}
		return Selects.builder()
				.ids(finalSelects.getIds())
				.validityDate(finalSelects.getValidityDate())
				.stratificationDate(finalSelects.getStratificationDate())
				.sqlSelects(getFinalAggregatedSelects(finalSelects, dialect))
				.build();
	}

	private List<? extends FieldWrapper<?>> getFinalAggregatedSelects(Selects finalSelects, CompilerDialect dialect) {
		return finalSelects.getSqlSelects().stream()
				.flatMap(sqlSelect -> sqlSelect.aggregateForFinalQuery(dialect).stream())
				.map(this::toFieldWrapper)
				.toList();
	}

	private FieldWrapper<?> toFieldWrapper(Field<?> field) {
		return new FieldWrapper<>(field);
	}

	private List<Field<?>> getFinalGroupBySelects(Selects preFinalSelects) {
		return preFinalSelects.getIds().toFields();
	}

}
