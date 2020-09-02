package com.bakdata.conquery.models.forms.managed;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Getter @RequiredArgsConstructor
public class RelativeFormQueryPlan implements QueryPlan {

	private static final int DATE_RANGE_SUB_RESULT = 3;
	private static final int EVENTDATE = 2;
	private static final int FEATURE_DATE_RANGE = 3;
	private static final int OUTCOME_DATE_RANGE = 4;

	private final QueryPlan query;
	private final ArrayConceptQueryPlan featurePlan;
	private final ArrayConceptQueryPlan outcomePlan;
	private final TemporalSampler indexSelector;
	private final IndexPlacement indexPlacement;
	private final int timeCountBefore;
	private final int timeCountAfter;
	private final DateContextMode timeUnit;
	private final List<DateContextMode> resolutions;

	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
		EntityResult preResult = query.execute(ctx, entity);
		if (preResult.isFailed() || !preResult.isContained()) {
			return preResult;
		}

		SinglelineContainedEntityResult contained = (SinglelineContainedEntityResult) preResult;
		CDateSet dateSet = CDateSet.parse(Objects.toString(contained.getValues()[0]));
		final OptionalInt sampled = indexSelector.sample(dateSet);

		// dateset is empty or sampling failed.
		if (!sampled.isPresent()) {
			log.warn("Sampled empty result for Entity[{}]: `{}({})`", contained.getEntityId(), indexSelector, dateSet);
			return preResult;
		}

		int sample = sampled.getAsInt();
		List<DateContext> contexts = DateContext
			.generateRelativeContexts(sample, indexPlacement, timeCountBefore, timeCountAfter, timeUnit, resolutions);

		FormQueryPlan featureSubquery = executeSubQuery(ctx, FeatureGroup.FEATURE, entity, contexts);
		FormQueryPlan outcomeSubquery = executeSubQuery(ctx, FeatureGroup.OUTCOME, entity, contexts);

		EntityResult featureResult = featureSubquery.execute(ctx, entity);
		EntityResult outcomeResult = outcomeSubquery.execute(ctx, entity);

		// on fail return failed result
		if (featureResult.isFailed()) {
			return featureResult;
		}
		if (outcomeResult.isFailed()) {
			return outcomeResult;
		}

		// Check if the result is processible (type is multiline or not contained)
		assertProcessible(featureResult);
		assertProcessible(outcomeResult);

		if (!featureResult.isContained() && !outcomeResult.isContained()) {
			// if both, feature and outcome are not contained fast quit.
			return EntityResult.notContained();
		}

		// determine result length and check against aggregators in query
		int featureLength = determineResultWidth(featureSubquery, featureResult);
		int outcomeLength = determineResultWidth(outcomeSubquery, outcomeResult);

		/*
		 * Whole result is the concatenation of the subresults. The final output format
		 * combines resolution info, index and eventdate of both sub queries. The
		 * feature/outcome sub queries are of in form of: [RESOLUTION], [INDEX],
		 * [EVENTDATE], [FEATURE/OUTCOME_DR], [FEATURE/OUTCOME_SELECTS]... The wanted
		 * format is: [RESOLUTION], [INDEX], [EVENTDATE], [FEATURE_DR], [OUTCOME_DR],
		 * [FEATURE_SELECTS]... , [OUTCOME_SELECTS]
		 */
		int size = featureLength + outcomeLength - 3/* ^= [RESOLUTION], [INDEX], [EVENTDATE] */;

		int resultStartIndex = 0;
		List<Object[]> values = new ArrayList<>();
		if (hasCompleteDateContexts(contexts)) {
			// merge a line for the complete daterange, when two dateContext were generated
			// that don't target the same feature group,
			// which would be a mistake by the generation
			// Since the DateContexts are primarily ordered by their coarseness and COMPLETE
			// is the coarsed resolution it must be at the first
			// to indexes of the list.
			Object[] mergedFull = new Object[size];
			if (featureResult.isContained()) {
				setFeatureValues(mergedFull, ((MultilineContainedEntityResult) featureResult).getValues().get(resultStartIndex));
			}
			if (outcomeResult.isContained()) {
				setOutcomeValues(
					mergedFull,
					((MultilineContainedEntityResult) outcomeResult).getValues().get(resultStartIndex),
					featureLength);
			}
			values.add(mergedFull);
			resultStartIndex++;
		}

		// append all other lines directly
		if (featureResult.isContained()) {
			MultilineContainedEntityResult multiresult = ((MultilineContainedEntityResult) featureResult);
			for (int i = resultStartIndex; i < multiresult.getValues().size(); i++) {
				Object[] result = new Object[size];
				setFeatureValues(result, multiresult.getValues().get(i));
				values.add(result);
			}
		}
		if (outcomeResult.isContained()) {
			MultilineContainedEntityResult multiresult = ((MultilineContainedEntityResult) outcomeResult);
			for (int i = resultStartIndex; i < multiresult.getValues().size(); i++) {
				Object[] result = new Object[size];
				setOutcomeValues(result, multiresult.getValues().get(i), featureLength);
				values.add(result);
			}
		}
		return EntityResult.multilineOf(entity.getId(), values);
	}

	private int determineResultWidth(FormQueryPlan subquery, EntityResult subResult) {
		// This is sufficient for NOT_CONTAINTED subresults
		int resultWidth = subquery.columnCount();
		// When it's a contained result also check whether the result really has the awaited width
		if (subResult.isContained()) {
			int resultColumnCount = subResult.asContained().columnCount();
			if(resultColumnCount != resultWidth) {				
				throw new IllegalStateException(String
					.format("Aggregator number (%d) and result number (%d) are not the same", resultWidth, resultColumnCount));
			}
		}
		return resultWidth;
	}

	private void assertProcessible(EntityResult result) {
		if (!(result instanceof MultilineContainedEntityResult) && result.isContained()) {
			throw new IllegalStateException(String.format(
				"The relative form queryplan only handles MultilineContainedEntityResult and NotContainedEntityResults. Was %s",
				result.getClass()));
		}
	}

	private boolean hasCompleteDateContexts(List<DateContext> contexts) {
		return contexts.size()>=2
			&& contexts.get(0).getSubdivisionMode().equals(DateContextMode.COMPLETE)
			&& contexts.get(1).getSubdivisionMode().equals(DateContextMode.COMPLETE)
			&& !contexts.get(0).getFeatureGroup().equals(contexts.get(1).getFeatureGroup());
	}

	private FormQueryPlan executeSubQuery(QueryExecutionContext ctx, FeatureGroup featureGroup, Entity entity, List<DateContext> contexts) {
		List<DateContext> list = new ArrayList<>(contexts);
		list.removeIf(dctx -> dctx.getFeatureGroup() != featureGroup);

		ArrayConceptQueryPlan subPlan = featureGroup == FeatureGroup.FEATURE ? featurePlan : outcomePlan;

		return new FormQueryPlan(list,subPlan);
	}
	
	private void setFeatureValues(Object[] result, Object[] value) {
		// copy everything up to including index
		for (int i = 0; i <= EVENTDATE; i++) {
			result[i] = value[i];
		}
		// copy daterange
		result[FEATURE_DATE_RANGE] = value[DATE_RANGE_SUB_RESULT];
		System.arraycopy(value, DATE_RANGE_SUB_RESULT+1, result, OUTCOME_DATE_RANGE + 1, value.length - (DATE_RANGE_SUB_RESULT+1));
	}
	
	private void setOutcomeValues(Object[] result, Object[] value, int featureLength) {
		// copy everything up to including index
		for (int i = 0; i <= EVENTDATE; i++) {
			result[i] = value[i];
		}
		// copy daterange
		result[OUTCOME_DATE_RANGE] = value[DATE_RANGE_SUB_RESULT];
		System.arraycopy(value, DATE_RANGE_SUB_RESULT+1, result, 1 + featureLength, value.length - (DATE_RANGE_SUB_RESULT+1));
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
			resolutions
		);
		return copy;
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return query.isOfInterest(entity) || featurePlan.isOfInterest(entity) || outcomePlan.isOfInterest(entity);
	}
}
