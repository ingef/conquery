package com.bakdata.conquery.models.forms.managed;

import java.util.*;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.forms.util.ResultModifier;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.*;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter @RequiredArgsConstructor
public class RelativeFormQueryPlan implements QueryPlan<MultilineEntityResult> {

	// Position of fixed columns in the result. (This is without identifier column[s], they are added upon result rendering)
	private static final int RESOLUTION_POS = 0;
	private static final int INDEX_POS = 1;
	private static final int EVENTDATE_POS = 2;
	// Position of fixed columns in the sub result.
	private static final int SUB_RESULT_DATE_RANGE_POS = 3;

	private final QueryPlan<?> query;
	private final ArrayConceptQueryPlan featurePlan;
	private final ArrayConceptQueryPlan outcomePlan;
	private final TemporalSampler indexSelector;
	private final IndexPlacement indexPlacement;
	private final int timeCountBefore;
	private final int timeCountAfter;
	private final DateContext.CalendarUnit timeUnit;
	private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignmentMap;

	private final transient List<FormQueryPlan> featureSubqueries = new ArrayList<>();
	private final transient List<FormQueryPlan> outcomeSubqueries = new ArrayList<>();

	@Override
	public Optional<MultilineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {

		// Don't set the query date aggregator here because the subqueries should set their aggregator independently

		Optional<? extends EntityResult> preResult = query.execute(ctx, entity);

		if (preResult.isEmpty()) {
			return Optional.empty();
		}

		int size = calculateCompleteLength();
		EntityResult contained = preResult.get();
		// Gather all validity dates from prerequisite
		CDateSet dateSet = query.getValidityDateAggregator().map(Aggregator::getAggregationResult).orElseGet(CDateSet::create);

		final OptionalInt sampled = indexSelector.sample(dateSet);

		// dateset is empty or sampling failed.
		if (sampled.isEmpty()) {
			log.warn("Sampled empty result for Entity[{}]: `{}({})`", contained.getEntityId(), indexSelector, dateSet);
			List<Object[]> results = new ArrayList<>();
			results.add(new Object[size]);
			return Optional.of(
					ResultModifier.modify(new MultilineEntityResult(entity.getId(), results), ResultModifier.existAggValuesSetterFor(getAggregators(), OptionalInt.of(getFirstAggregatorPosition())))
			);
		}

		int sample = sampled.getAsInt();
		List<DateContext> contexts = DateContext
			.generateRelativeContexts(sample, indexPlacement, timeCountBefore, timeCountAfter, timeUnit, resolutionsAndAlignmentMap);
		
		// create feature and outcome plans
		FormQueryPlan featureSubquery = createSubQuery(featurePlan, contexts, FeatureGroup.FEATURE);
		FormQueryPlan outcomeSubquery = createSubQuery(outcomePlan, contexts, FeatureGroup.OUTCOME);

		featureSubqueries.add(featureSubquery);
		outcomeSubqueries.add(outcomeSubquery);

		// determine result length and check against aggregators in query
		int featureLength = featureSubquery.columnCount();
		int outcomeLength = outcomeSubquery.columnCount();



		Optional<MultilineEntityResult> featureResult = featureSubquery.execute(ctx, entity);
		Optional<MultilineEntityResult> outcomeResult = outcomeSubquery.execute(ctx, entity);

		// determine result length and check against aggregators in query
		checkResultWidth(featureResult.get(), featureLength);
		checkResultWidth(outcomeResult.get(), outcomeLength);

		List<Object[]> featureResultValues = featureResult.get().getValues();
		List<Object[]> outcomeResultValues = outcomeResult.get().getValues();

		int resultStartIndex = 0;
		List<Object[]> values = new ArrayList<>();
		if (hasCompleteDateContexts(contexts)) {
			// merge a line for the complete daterange, when two dateContext were generated
			// that don't target the same feature group,
			// which would be a mistake by the generation
			// Since the DateContexts are primarily ordered by their coarseness and COMPLETE
			// is the most coarse resolution it must be at the first
			// to indexes of the list.
			Object[] mergedFull = new Object[size];

			if (featurePlan.getAggregatorSize() > 0) {
				setFeatureValues(mergedFull, featureResultValues.get(resultStartIndex));
			}

			if (outcomePlan.getAggregatorSize() > 0) {
				setOutcomeValues(
						mergedFull,
						outcomeResultValues.get(resultStartIndex)
				);
			}

			values.add(mergedFull);
			resultStartIndex++;
		}

		// append all other lines directly
		for (int i = resultStartIndex; i < featureResultValues.size(); i++) {
			Object[] result = new Object[size];
			setFeatureValues(result, featureResultValues.get(i));
			values.add(result);
		}

		for (int i = resultStartIndex; i < outcomeResultValues.size(); i++) {
			Object[] result = new Object[size];
			setOutcomeValues(result, outcomeResultValues.get(i));
			values.add(result);
		}

		return Optional.of(new MultilineEntityResult(entity.getId(), values));
	}


	private int getFeatureDateRangePosition(){
		return featurePlan.getAggregatorSize() > 0? 3 : -1;
	}

	private int getOutcomeDateRangePosition(){
		if (outcomePlan.getAggregatorSize() > 0){
			return featurePlan.getAggregatorSize() > 0? 4 : 3;
		}
		return -1;
	}

	private int getFirstAggregatorPosition(){
		if (outcomePlan.getAggregatorSize() <= 0 && featurePlan.getAggregatorSize() <= 0) {
			throw new ConqueryError.ExecutionProcessingError();
		}
		if (outcomePlan.getAggregatorSize() > 0 && featurePlan.getAggregatorSize() <= 0
				|| featurePlan.getAggregatorSize() > 0 && outcomePlan.getAggregatorSize() <= 0){
			return 4;
		}
		return 5;
	}

	private int calculateCompleteLength() {
		/*
		 * Whole result is the concatenation of the subresults. The final output format
		 * combines resolution info, index and eventdate of both sub queries. The
		 * feature/outcome sub queries are of in form of: [RESOLUTION], [INDEX], [EVENTDATE], [FEATURE/OUTCOME_DR], [FEATURE/OUTCOME_SELECTS]...
		 * The wanted format is: [RESOLUTION], [INDEX], [EVENTDATE], [FEATURE_DR], [OUTCOME_DR], [FEATURE_SELECTS]... , [OUTCOME_SELECTS]
		 */

		return getFirstAggregatorPosition() + featurePlan.getAggregatorSize() + outcomePlan.getAggregatorSize();
		//return featureLength + outcomeLength - 3/* ^= [RESOLUTION], [INDEX], [EVENTDATE] */;
	}

	private int checkResultWidth(EntityResult subResult, int resultWidth) {
		int resultColumnCount = subResult.columnCount();

		if(resultColumnCount != resultWidth) {
			throw new IllegalStateException(String
				.format("Aggregator number (%d) and result number (%d) are not the same", resultWidth, resultColumnCount));
		}
		return resultWidth;
	}


	private boolean hasCompleteDateContexts(List<DateContext> contexts) {
		if(contexts.isEmpty()){
			return false;
		}
		if (featurePlan.getAggregatorSize() > 0 && outcomePlan.getAggregatorSize() > 0) {
			// We have features and outcomes check if both have complete date ranges (they should be at the beginning of the list)
			return contexts.size()>=2
				&& contexts.get(0).getSubdivisionMode().equals(DateContext.Resolution.COMPLETE)
				&& contexts.get(1).getSubdivisionMode().equals(DateContext.Resolution.COMPLETE)
				&& !contexts.get(0).getFeatureGroup().equals(contexts.get(1).getFeatureGroup());
		}
		// Otherwise, if only features or outcomes are given check the first date context. The empty feature/outcome query
		// will still return an empty result which will be merged with to a complete result.
		return contexts.get(0).getSubdivisionMode().equals(DateContext.Resolution.COMPLETE);
	}

	private FormQueryPlan createSubQuery(ArrayConceptQueryPlan subPlan, List<DateContext> contexts, FeatureGroup featureGroup) {
		List<DateContext> list = new ArrayList<>(contexts);
		list.removeIf(dctx -> dctx.getFeatureGroup() != featureGroup);

		return new FormQueryPlan(list, subPlan);
	}

	private void setFeatureValues(Object[] result, Object[] value) {
		// copy everything up to including index
		for (int i = 0; i <= EVENTDATE_POS; i++) {
			result[i] = value[i];
		}
		// copy daterange
		result[getFeatureDateRangePosition()] = value[SUB_RESULT_DATE_RANGE_POS];
		System.arraycopy(value, SUB_RESULT_DATE_RANGE_POS+1, result, getFirstAggregatorPosition(), featurePlan.getAggregatorSize());
	}

	private void setOutcomeValues(Object[] result, Object[] value) {
		// copy everything up to including index
		for (int i = 0; i <= EVENTDATE_POS; i++) {
			if(result[i] != null){
				// Skip cells that where already filled from the feature result (in complete merge)
				continue;
			}
			result[i] = value[i];
		}
		// copy daterange
		result[getOutcomeDateRangePosition()] = value[SUB_RESULT_DATE_RANGE_POS];
		System.arraycopy(value, SUB_RESULT_DATE_RANGE_POS+1, result, getFirstAggregatorPosition() + featurePlan.getAggregatorSize(), outcomePlan.getAggregatorSize());
	}

	public List<Aggregator<?>> getAggregators() {
		return ImmutableList.copyOf(Iterables.concat(featurePlan.getAggregators(),outcomePlan.getAggregators()));
	}

	@Override
	public RelativeFormQueryPlan clone(CloneContext ctx) {
		RelativeFormQueryPlan copy = new RelativeFormQueryPlan(
			query.clone(ctx),
			featurePlan.clone(ctx),
			outcomePlan.clone(ctx),
			indexSelector,
			indexPlacement,
			timeCountBefore,
			timeCountAfter,
			timeUnit,
			resolutionsAndAlignmentMap
		);
		return copy;
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return query.isOfInterest(entity) || featurePlan.isOfInterest(entity) || outcomePlan.isOfInterest(entity);
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		DateAggregator agg = new DateAggregator(DateAggregationAction.MERGE);

		for (FormQueryPlan featureSubquery : featureSubqueries) {
			featureSubquery.getValidityDateAggregator().ifPresent(agg::register);
		}

		for (FormQueryPlan outcomeSubquery : outcomeSubqueries) {
			outcomeSubquery.getValidityDateAggregator().ifPresent(agg::register);
		}

		return agg.hasChildren() ? Optional.of(agg) : Optional.empty();
	}
}
