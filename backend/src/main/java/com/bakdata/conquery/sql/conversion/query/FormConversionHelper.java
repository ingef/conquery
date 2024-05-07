package com.bakdata.conquery.sql.conversion.query;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.FormCteStep;
import com.bakdata.conquery.sql.conversion.forms.FormType;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.ConqueryJoinType;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class FormConversionHelper {

	private final QueryStepTransformer queryStepTransformer;

	/**
	 * Converts the given {@link Query} and creates another {@link QueryStep} on top which extracts only the primary id and the validity dates.
	 */
	public QueryStep convertPrerequisite(Query query, ConversionContext context) {

		ConversionContext withConvertedPrerequisite = context.getNodeConversions().convert(query, context);
		Preconditions.checkArgument(withConvertedPrerequisite.getQuerySteps().size() == 1, "Base query conversion should produce exactly 1 QueryStep");
		QueryStep convertedPrerequisite = withConvertedPrerequisite.getQuerySteps().get(0);

		Selects prerequisiteSelects = convertedPrerequisite.getQualifiedSelects();
		// we keep the primary column and the validity date
		Selects selects = Selects.builder()
								 .ids(new SqlIdColumns(prerequisiteSelects.getIds().getPrimaryColumn()))
								 .validityDate(prerequisiteSelects.getValidityDate())
								 .build();

		// we want to keep each primary column and the corresponding distinct validity date ranges
		List<Field<?>> groupByFields = Stream.concat(
													 Stream.of(prerequisiteSelects.getIds().getPrimaryColumn()),
													 prerequisiteSelects.getValidityDate().stream().flatMap(validityDate -> validityDate.toFields().stream())
											 )
											 .collect(Collectors.toList());

		// filter out entries with a null validity date
		Condition dateNotNullCondition = prerequisiteSelects.getValidityDate().get().isNotNull();

		return QueryStep.builder()
						.cteName(FormCteStep.EXTRACT_IDS.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(convertedPrerequisite.getCteName()))
						.conditions(List.of(dateNotNullCondition))
						.groupBy(groupByFields)
						.predecessors(List.of(convertedPrerequisite))
						.build();
	}

	public ConversionContext convertForm(
			FormType formType,
			QueryStep stratificationTable,
			ArrayConceptQuery features,
			List<ResultInfo> resultInfos,
			ConversionContext context
	) {
		// feature conversion
		ConversionContext childContext = context.createChildContext().withStratificationTable(stratificationTable);
		for (ConceptQuery conceptQuery : features.getChildQueries()) {
			childContext = context.getNodeConversions().convert(conceptQuery, childContext);
		}

		// child context contains the converted feature's QuerySteps
		List<QueryStep> queriesToJoin = childContext.getQuerySteps();
		QueryStep joinedFeatures = QueryStepJoiner.joinSteps(queriesToJoin, ConqueryJoinType.OUTER_JOIN, DateAggregationAction.BLOCK, context);
		return createFinalSelect(formType, stratificationTable, joinedFeatures, resultInfos, context);
	}

	/**
	 * Left-joins the full stratification table back with the converted feature tables to keep all the resolutions.
	 * <p>
	 * When converting features, we filter out rows where a subjects validity date and the stratification date do not overlap.
	 * Thus, the pre-final step might not contain an entry for each expected stratification window. That's why we need to left-join the converted
	 * features with the full stratification table again.
	 */
	private ConversionContext createFinalSelect(
			FormType formType,
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
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();

		QueryStep finalStep = QueryStep.builder()
									   .cteName(null)  // the final QueryStep won't be converted to a CTE
									   .selects(getFinalSelects(formType, stratificationTable, convertedFeatures, functionProvider))
									   .fromTable(joinedTable)
									   .predecessors(queriesToJoin)
									   .build();

		Select<Record> selectQuery = queryStepTransformer.toSelectQuery(finalStep);
		return context.withFinalQuery(new SqlQuery(selectQuery, resultInfos));
	}

	/**
	 * Selects the ID, resolution, index and date range from stratification table plus all explicit selects from the converted features step.
	 * For {@link FormType#RELATIVE}, the {@link FeatureGroup} will be set too.
	 */
	private static Selects getFinalSelects(
			FormType formType,
			QueryStep stratificationTable,
			QueryStep convertedFeatures,
			SqlFunctionProvider functionProvider
	) {
		Selects preFinalSelects = convertedFeatures.getQualifiedSelects();

		Selects stratificationSelects = stratificationTable.getQualifiedSelects();
		SqlIdColumns ids = stratificationSelects.getIds().forFinalSelect();
		Field<String> daterangeConcatenated = functionProvider.daterangeStringExpression(stratificationSelects.getStratificationDate().get())
															  .as(SharedAliases.STRATIFICATION_BOUNDS.getAlias());

		Selects.SelectsBuilder selects = Selects.builder()
												.ids(ids)
												.validityDate(Optional.empty())
												.stratificationDate(Optional.of(ColumnDateRange.of(daterangeConcatenated)));

		if (formType != FormType.RELATIVE) {
			return selects.sqlSelects(preFinalSelects.getSqlSelects()).build();
		}

		// relative forms have FeatureGroup information after the stratification date and before all other selects
		Field<Integer> indexField = DSL.field(DSL.name(stratificationTable.getCteName(), SharedAliases.INDEX.getAlias()), Integer.class);
		Field<String> scope = DSL.when(indexField.isNull().or(indexField.lessThan(0)), DSL.val(FeatureGroup.FEATURE.toString()))
								 .otherwise(DSL.val(FeatureGroup.OUTCOME.toString()))
								 .as(SharedAliases.OBSERVATION_SCOPE.getAlias());

		return selects.sqlSelect(new FieldWrapper<>(scope))
					  .sqlSelects(preFinalSelects.getSqlSelects())
					  .build();
	}

}
