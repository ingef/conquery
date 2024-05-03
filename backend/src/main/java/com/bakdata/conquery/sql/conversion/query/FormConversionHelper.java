package com.bakdata.conquery.sql.conversion.query;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.StratificationTableFactory;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.ConqueryJoinType;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.TableLike;

@RequiredArgsConstructor
public class FormConversionHelper {

	private final QueryStepTransformer queryStepTransformer;

	public ConversionContext convertForm(
			QueryStep convertedPrerequisite,
			List<ExportForm.ResolutionAndAlignment> resolutionAndAlignments,
			ArrayConceptQuery features,
			List<ResultInfo> resultInfos,
			ConversionContext context
	) {
		// create stratification table
		StratificationTableFactory tableFactory = new StratificationTableFactory(convertedPrerequisite, context);
		QueryStep stratificationTable = tableFactory.createStratificationTable(resolutionAndAlignments);

		// feature conversion
		ConversionContext childContext = context.createChildContext().withStratificationTable(stratificationTable);
		for (ConceptQuery conceptQuery : features.getChildQueries()) {
			childContext = context.getNodeConversions().convert(conceptQuery, childContext);
		}

		// child context contains the converted feature's QuerySteps
		List<QueryStep> queriesToJoin = childContext.getQuerySteps();

		// only 1 converted feature means we don't have to join the feature queries
		if (queriesToJoin.size() == 1) {
			QueryStep convertedFeature = queriesToJoin.get(0);
			return createFinalSelect(stratificationTable, convertedFeature, resultInfos, context);
		}
		QueryStep joinedFeatures = QueryStepJoiner.joinSteps(queriesToJoin, ConqueryJoinType.OUTER_JOIN, DateAggregationAction.BLOCK, context);
		return createFinalSelect(stratificationTable, joinedFeatures, resultInfos, context);
	}

	/**
	 * Left-joins the full stratification table back with the converted feature tables to keep all the resolutions.
	 * <p>
	 * When converting features, we filter out rows where a subjects validity date and the stratification date do not overlap.
	 * Thus, the pre-final step might not contain an entry for each expected stratification window. That's why we need to left-join the converted
	 * features with the full stratification table again.
	 */
	public ConversionContext createFinalSelect(
			QueryStep stratificationTable,
			QueryStep convertedFeatures,
			List<ResultInfo> resultInfos,
			ConversionContext context
	) {
		Preconditions.checkArgument(
				stratificationTable.getSelects().getStratificationDate().isPresent() && convertedFeatures.getSelects().getStratificationDate().isPresent(),
				"Expecting stratification table and converted features table to contain a stratification date"
		);

		List<QueryStep> queriesToJoin = List.of(stratificationTable, convertedFeatures);
		TableLike<Record> joinedTable = QueryStepJoiner.constructJoinedTable(queriesToJoin, ConqueryJoinType.LEFT_JOIN, context);

		QueryStep finalStep = QueryStep.builder()
									   .cteName(null)  // the final QueryStep won't be converted to a CTE
									   .selects(getFinalSelects(stratificationTable, convertedFeatures, context.getSqlDialect().getFunctionProvider()))
									   .fromTable(joinedTable)
									   .predecessors(queriesToJoin)
									   .build();

		Select<Record> selectQuery = queryStepTransformer.toSelectQuery(finalStep);
		return context.withFinalQuery(new SqlQuery(selectQuery, resultInfos));
	}

	/**
	 * Selects the ID, resolution, index and date range from stratification table plus all explicit selects from the converted features step.
	 */
	private static Selects getFinalSelects(QueryStep stratificationTable, QueryStep convertedFeatures, SqlFunctionProvider functionProvider) {

		Selects preFinalSelects = convertedFeatures.getQualifiedSelects();

		Selects stratificationSelects = stratificationTable.getQualifiedSelects();
		SqlIdColumns ids = stratificationSelects.getIds().forFinalSelect();
		Field<String> daterangeConcatenated = functionProvider.daterangeStringExpression(stratificationSelects.getStratificationDate().get())
															  .as(SharedAliases.STRATIFICATION_BOUNDS.getAlias());

		return Selects.builder()
					  .ids(ids)
					  .validityDate(Optional.empty())
					  .stratificationDate(Optional.of(ColumnDateRange.of(daterangeConcatenated)))
					  .sqlSelects(preFinalSelects.getSqlSelects())
					  .build();
	}

}
