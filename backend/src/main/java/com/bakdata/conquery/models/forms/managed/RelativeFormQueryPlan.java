package com.bakdata.conquery.models.forms.managed;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.forms.util.ResultModifier;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.DateAggregator;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
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
	private final CalendarUnit timeUnit;
	private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignmentMap;

	private FormQueryPlan featureSubquery = null;
	private FormQueryPlan outcomeSubquery = null;

	@Override
	public void init(QueryExecutionContext ctxt, Entity entity) {
		query.init(ctxt, entity);
		featurePlan.init(ctxt, entity);
		outcomePlan.init(ctxt, entity);

		featureSubquery = null;
		outcomeSubquery = null;
	}

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
		CDateSet dateSet = query.getValidityDateAggregator().map(Aggregator::createAggregationResult).orElseGet(CDateSet::create);

		final OptionalInt sampled = indexSelector.sample(dateSet);

		// dateset is empty or sampling failed.
		if (sampled.isEmpty()) {
			log.warn("Sampled empty result for Entity[{}]: `{}({})`", contained.getEntityId(), indexSelector, dateSet);
			List<Object[]> results = new ArrayList<>();
			results.add(new Object[size]);
			return Optional.of(
					ResultModifier.modify(new MultilineEntityResult(entity.getId(), results), ResultModifier.existAggValuesSetterFor(getAggregators(), OptionalInt
							.of(getFirstAggregatorPosition())))
			);
		}

		int sample = sampled.getAsInt();
		List<DateContext> contexts =
				DateContext.generateRelativeContexts(sample, indexPlacement, timeCountBefore, timeCountAfter, timeUnit, resolutionsAndAlignmentMap);

		// create feature and outcome plans
		featureSubquery = createSubQuery(featurePlan, contexts, FeatureGroup.FEATURE);
		outcomeSubquery = createSubQuery(outcomePlan, contexts, FeatureGroup.OUTCOME);



		// determine result length and check against aggregators in query
		int featureLength = featureSubquery.columnCount();
		int outcomeLength = outcomeSubquery.columnCount();

		featureSubquery.init(ctx, entity);
		outcomeSubquery.init(ctx, entity);

		Optional<MultilineEntityResult> featureResult = featureSubquery.execute(ctx, entity);
		Optional<MultilineEntityResult> outcomeResult = outcomeSubquery.execute(ctx, entity);

		// determine result length and check against aggregators in query
		assertResultWidth(featureResult.get(), featureLength);
		assertResultWidth(outcomeResult.get(), outcomeLength);

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

			if (featurePlan.getAggregatorSize() > 0) {
				Object[] result = new Object[size];
				setFeatureValues(featureSubquery.getConstantCount(),result, featureResultValues.get(resultStartIndex));
				values.add(result);
			}


			if (outcomePlan.getAggregatorSize() > 0) {
				Object[] result = new Object[size];
				setOutcomeValues(outcomeSubquery.getConstantCount(), result, outcomeResultValues.get(resultStartIndex));
				values.add(result);
			}
			resultStartIndex++;
		}

		// append all other lines directly
		for (int i = resultStartIndex; i < featureResultValues.size(); i++) {
			Object[] result = new Object[size];
			setFeatureValues(featureSubquery.getConstantCount(), result, featureResultValues.get(i));
			values.add(result);
		}

		for (int i = resultStartIndex; i < outcomeResultValues.size(); i++) {
			Object[] result = new Object[size];
			setOutcomeValues(outcomeSubquery.getConstantCount(), result, outcomeResultValues.get(i));
			values.add(result);
		}

		return Optional.of(new MultilineEntityResult(entity.getId(), values));
	}


	private int getDateRangePosition() {
		return featurePlan.getAggregatorSize() > 0 ? 3 : -1;
	}

	private int getFirstAggregatorPosition() {
		return 5;
		// TODO
//		if (outcomePlan.getAggregatorSize() <= 0 && featurePlan.getAggregatorSize() <= 0) {
//			throw new ConqueryError.ExecutionProcessingError();
//		}
//		if (outcomePlan.getAggregatorSize() > 0 && featurePlan.getAggregatorSize() <= 0
//			|| featurePlan.getAggregatorSize() > 0 && outcomePlan.getAggregatorSize() <= 0) {
//			// If either feature or outcome is given, we don't output the observation scope, since it would be the same everywhere
//			return 3;
//		}
//		// We need an extra column for the observation scope
//		return 4;
	}

	/**
	 * Whole result is the concatenation of the subresults. The final output format
	 * combines resolution info, index and eventdate of both sub queries. The
	 * feature/outcome sub queries are of in form of: [RESOLUTION], [INDEX], [EVENTDATE], [FEATURE/OUTCOME_DR], [FEATURE/OUTCOME_SELECTS]...
	 * The wanted format is: [RESOLUTION], [INDEX], [EVENTDATE], [FEATURE_DR], [OUTCOME_DR], [FEATURE_SELECTS]... , [OUTCOME_SELECTS]
	 */
	private int calculateCompleteLength() {

		return getFirstAggregatorPosition() + featurePlan.getAggregatorSize() + outcomePlan.getAggregatorSize();
		//return featureLength + outcomeLength - 3/* ^= [RESOLUTION], [INDEX], [EVENTDATE] */;
	}

	private void assertResultWidth(EntityResult subResult, int resultWidth) {
		int resultColumnCount = subResult.columnCount();

		if (resultColumnCount == resultWidth) {
			return;
		}

		throw new IllegalStateException(String.format("Aggregator number (%d) and result number (%d) are not the same", resultWidth, resultColumnCount));
	}


	private boolean hasCompleteDateContexts(List<DateContext> contexts) {
		if (contexts.isEmpty()) {
			return false;
		}

		if (featurePlan.getAggregatorSize() <= 0 || outcomePlan.getAggregatorSize() <= 0) {
			// Otherwise, if only features or outcomes are given check the first date context. The empty feature/outcome query
			// will still return an empty result which will be merged with to a complete result.
			return Resolution.COMPLETE.equals(contexts.get(0).getSubdivisionMode());
		}

		// We have features and outcomes check if both have complete date ranges (they should be at the beginning of the list)
		return contexts.size() >= 2
			   && Resolution.COMPLETE.equals(contexts.get(0).getSubdivisionMode())
			   && Resolution.COMPLETE.equals(contexts.get(1).getSubdivisionMode())
			   && !contexts.get(0).getFeatureGroup().equals(contexts.get(1).getFeatureGroup());
	}

	private static FormQueryPlan createSubQuery(ArrayConceptQueryPlan subPlan, List<DateContext> contexts, FeatureGroup featureGroup) {
		List<DateContext> list = new ArrayList<>(contexts);
		list.removeIf(dctx -> dctx.getFeatureGroup() != featureGroup);

		return new FormQueryPlan(list, subPlan);
	}

	private void setFeatureValues(int constantCount, Object[] result, Object[] value) {
		// copy everything up to including scope
		System.arraycopy(value, 0, result, 0, constantCount);

		System.arraycopy(value, constantCount, result, getFirstAggregatorPosition(), featurePlan.getAggregatorSize());
	}

	private void setOutcomeValues(int constantCount, Object[] result, Object[] value) {
		// copy everything up to including scope
		System.arraycopy(value, 0, result, 0, constantCount);

		System.arraycopy(value, constantCount, result, getFirstAggregatorPosition()
																	   + featurePlan.getAggregatorSize(), outcomePlan.getAggregatorSize());
	}

	public List<Aggregator<?>> getAggregators() {
		return ImmutableList.copyOf(Iterables.concat(featurePlan.getAggregators(), outcomePlan.getAggregators()));
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return query.isOfInterest(entity) || featurePlan.isOfInterest(entity) || outcomePlan.isOfInterest(entity);
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		DateAggregator agg = new DateAggregator(DateAggregationAction.MERGE);

		featureSubquery.getValidityDateAggregator().ifPresent(agg::register);
		outcomeSubquery.getValidityDateAggregator().ifPresent(agg::register);

		return agg.hasChildren() ? Optional.of(agg) : Optional.empty();
	}
}
