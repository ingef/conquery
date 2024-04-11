package com.bakdata.conquery.sql.conversion.query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.FormCteStep;
import com.bakdata.conquery.sql.conversion.forms.StratificationTableFactory;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.LogicalOperation;
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
public class AbsoluteFormQueryConverter implements NodeConverter<AbsoluteFormQuery> {

	private final QueryStepTransformer queryStepTransformer;

	@Override
	public Class<? extends AbsoluteFormQuery> getConversionClass() {
		return AbsoluteFormQuery.class;
	}

	@Override
	public ConversionContext convert(AbsoluteFormQuery form, ConversionContext context) {

		// base population query conversion
		QueryStep prerequisite = convertPrerequisite(form.getQuery(), form.getDateRange(), context);

		// creating stratification tables
		StratificationTableFactory tableFactory = new StratificationTableFactory(prerequisite, context);
		QueryStep stratificationTable = tableFactory.createStratificationTable(form);

		// feature conversion
		ConversionContext childContext = convertFeatures(form, context, stratificationTable);

		List<QueryStep> queriesToJoin = childContext.getQuerySteps();
		// only 1 converted feature
		if (queriesToJoin.size() == 1) {
			QueryStep convertedFeature = queriesToJoin.get(0);
			return createFinalSelect(form, stratificationTable, convertedFeature, childContext);
		}
		QueryStep joinedFeatures = QueryStepJoiner.joinSteps(queriesToJoin, LogicalOperation.OR, DateAggregationAction.BLOCK, context);
		return createFinalSelect(form, stratificationTable, joinedFeatures, childContext);
	}

	private static QueryStep convertPrerequisite(Query query, Range<LocalDate> formDateRange, ConversionContext context) {

		ConversionContext withConvertedPrerequisite = context.getNodeConversions().convert(query, context);
		Preconditions.checkArgument(withConvertedPrerequisite.getQuerySteps().size() == 1, "Base query conversion should produce exactly 1 QueryStep");
		QueryStep convertedPrerequisite = withConvertedPrerequisite.getQuerySteps().get(0);

		ColumnDateRange bounds = context.getSqlDialect()
										.getFunctionProvider()
										.forCDateRange(CDateRange.of(formDateRange)).as(SharedAliases.STRATIFICATION_BOUNDS.getAlias());

		Selects prerequisiteSelects = convertedPrerequisite.getQualifiedSelects();
		// we only keep the primary column for the upcoming form
		Selects selects = Selects.builder()
								 .ids(new SqlIdColumns(prerequisiteSelects.getIds().getPrimaryColumn()))
								 .stratificationDate(Optional.of(bounds))
								 .build();

		return QueryStep.builder()
						.cteName(FormCteStep.EXTRACT_IDS.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(convertedPrerequisite.getCteName()))
						.groupBy(selects.getIds().toFields()) // group by primary column to ensure max. 1 entry per subject
						.predecessors(List.of(convertedPrerequisite))
						.build();
	}

	private static ConversionContext convertFeatures(AbsoluteFormQuery form, ConversionContext context, QueryStep stratificationTable) {
		ConversionContext childContext = context.createChildContext().withStratificationTable(stratificationTable);
		for (ConceptQuery conceptQuery : form.getFeatures().getChildQueries()) {
			childContext = context.getNodeConversions().convert(conceptQuery, childContext);
		}
		return childContext;
	}

	/**
	 * Left-joins the full stratification table back with the converted feature tables to keep all the resolutions.
	 * <p>
	 * When converting features, we filter out rows where a subjects validity date and the stratification date do not overlap.
	 * Thus, the pre-final step might not contain an entry for each expected stratification window. That's why we need to left-join the converted
	 * features with the full stratification table again.
	 */
	private ConversionContext createFinalSelect(AbsoluteFormQuery form, QueryStep stratificationTable, QueryStep convertedFeatures, ConversionContext context) {

		List<QueryStep> queriesToJoin = List.of(stratificationTable, convertedFeatures);
		TableLike<Record> joinedTable = QueryStepJoiner.constructJoinedTable(queriesToJoin, LogicalOperation.LEFT_JOIN, context);

		QueryStep finalStep = QueryStep.builder()
									   .cteName(null)  // the final QueryStep won't be converted to a CTE
									   .selects(getFinalSelects(stratificationTable, convertedFeatures, context.getSqlDialect().getFunctionProvider()))
									   .fromTable(joinedTable)
									   .predecessors(queriesToJoin)
									   .build();

		Select<Record> selectQuery = queryStepTransformer.toSelectQuery(finalStep);
		return context.withFinalQuery(new SqlQuery(selectQuery, form.getResultInfos()));
	}

	/**
	 * Selects the ID, resolution, index and date range from stratification table plus all explicit selects from the converted features step.
	 */
	private static Selects getFinalSelects(QueryStep stratificationTable, QueryStep convertedFeatures, SqlFunctionProvider functionProvider) {

		Selects preFinalSelects = convertedFeatures.getQualifiedSelects();

		if (preFinalSelects.getStratificationDate().isEmpty() || !preFinalSelects.getIds().isWithStratification()) {
			throw new IllegalStateException("Expecting the pre-final CTE to contain a stratification date, resolution and index");
		}

		Selects stratificationSelects = stratificationTable.getQualifiedSelects();
		SqlIdColumns ids = stratificationSelects.getIds().forFinalSelect();
		Field<String> daterangeConcatenated = functionProvider.daterangeStringExpression(stratificationSelects.getStratificationDate().get())
															  .as(SharedAliases.STRATIFICATION_RANGE.getAlias());

		return Selects.builder()
					  .ids(ids)
					  .validityDate(Optional.empty())
					  .stratificationDate(Optional.of(ColumnDateRange.of(daterangeConcatenated)))
					  .sqlSelects(preFinalSelects.getSqlSelects())
					  .build();
	}

}
