package com.bakdata.conquery.models.forms.managed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.models.common.BitMapCDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.forms.util.ResultModifier;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import com.google.common.collect.ImmutableList;
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
		
		// generate date contexts 
		BitMapCDateSet dateSet = BitMapCDateSet.parse(Objects.toString(contained.getValues()[0]));
		final OptionalInt sampled = indexSelector.sample(dateSet);
		
		// dateset is empty or sampling failed.
		if (sampled.isEmpty()) {
			log.warn("Sampled empty result for Entity[{}]: `{}({})`", contained.getEntityId(), indexSelector, dateSet);
			List<DateContext> contextsDummy = List.of(new DateContext(CDateRange.all(),FeatureGroup.FEATURE,null,LocalDate.ofEpochDay(0), DateContextMode.COMPLETE),new DateContext(CDateRange.all(),FeatureGroup.OUTCOME,null,LocalDate.ofEpochDay(0), DateContextMode.COMPLETE));
			// create feature and outcome plans
			FormQueryPlan featureSubqueryDummy = createSubQuery(featurePlan, contextsDummy, FeatureGroup.FEATURE);
			FormQueryPlan outcomeSubqueryDummy = createSubQuery(outcomePlan, contextsDummy, FeatureGroup.OUTCOME);
			// TODO remove this double wrapping 
			return EntityResult.multilineOf(entity.getId(), ResultModifier.modify(EntityResult.multilineOf(entity.getId(), ImmutableList.of(new Object[calculateCompleteLength(featureSubqueryDummy.columnCount(), outcomeSubqueryDummy.columnCount())])), UnaryOperator.identity()));
		}
		
		int sample = sampled.getAsInt();
		List<DateContext> contexts = DateContext
			.generateRelativeContexts(sample, indexPlacement, timeCountBefore, timeCountAfter, timeUnit, resolutions);
		
		// create feature and outcome plans
		FormQueryPlan featureSubquery = createSubQuery(featurePlan, contexts, FeatureGroup.FEATURE);
		FormQueryPlan outcomeSubquery = createSubQuery(outcomePlan, contexts, FeatureGroup.OUTCOME);

		// determine result length and check against aggregators in query
		int featureLength = featureSubquery.columnCount();
		int outcomeLength = outcomeSubquery.columnCount();

		int size = calculateCompleteLength(featureLength, outcomeLength);


		MultilineContainedEntityResult featureResult = featureSubquery.execute(ctx, entity);
		MultilineContainedEntityResult outcomeResult = outcomeSubquery.execute(ctx, entity);

		// on fail return failed result
		if (featureResult.isFailed()) {
			return featureResult;
		}
		if (outcomeResult.isFailed()) {
			return outcomeResult;
		}

		// determine result length and check against aggregators in query
		checkResultWidth(featureResult, featureLength);
		checkResultWidth(outcomeResult, outcomeLength);


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

			setFeatureValues(mergedFull, featureResult.getValues().get(resultStartIndex));

			setOutcomeValues(
					mergedFull,
					outcomeResult.getValues().get(resultStartIndex),
					featureLength
			);

			values.add(mergedFull);
			resultStartIndex++;
		}

		// append all other lines directly
		for (int i = resultStartIndex; i < featureResult.getValues().size(); i++) {
			Object[] result = new Object[size];
			setFeatureValues(result, featureResult.getValues().get(i));
			values.add(result);
		}

		for (int i = resultStartIndex; i < outcomeResult.getValues().size(); i++) {
			Object[] result = new Object[size];
			setOutcomeValues(result, outcomeResult.getValues().get(i), featureLength);
			values.add(result);
		}

		return EntityResult.multilineOf(entity.getId(), values);
	}

	private int calculateCompleteLength(int featureLength, int outcomeLength) {
		/*
		 * Whole result is the concatenation of the subresults. The final output format
		 * combines resolution info, index and eventdate of both sub queries. The
		 * feature/outcome sub queries are of in form of: [RESOLUTION], [INDEX],
		 * [EVENTDATE], [FEATURE/OUTCOME_DR], [FEATURE/OUTCOME_SELECTS]... The wanted
		 * format is: [RESOLUTION], [INDEX], [EVENTDATE], [FEATURE_DR], [OUTCOME_DR],
		 * [FEATURE_SELECTS]... , [OUTCOME_SELECTS]
		 */
		return featureLength + outcomeLength - 3/* ^= [RESOLUTION], [INDEX], [EVENTDATE] */;
	}

	private int checkResultWidth(EntityResult subResult, int resultWidth) {
		int resultColumnCount = subResult.asContained().columnCount();

		if(resultColumnCount != resultWidth) {
			throw new IllegalStateException(String
				.format("Aggregator number (%d) and result number (%d) are not the same", resultWidth, resultColumnCount));
		}
		return resultWidth;
	}


	private boolean hasCompleteDateContexts(List<DateContext> contexts) {
		return contexts.size()>=2
			&& contexts.get(0).getSubdivisionMode().equals(DateContextMode.COMPLETE)
			&& contexts.get(1).getSubdivisionMode().equals(DateContextMode.COMPLETE)
			&& !contexts.get(0).getFeatureGroup().equals(contexts.get(1).getFeatureGroup());
	}

	private FormQueryPlan createSubQuery(ArrayConceptQueryPlan subPlan, List<DateContext> contexts, FeatureGroup featureGroup) {
		List<DateContext> list = new ArrayList<>(contexts);
		list.removeIf(dctx -> dctx.getFeatureGroup() != featureGroup);

		return new FormQueryPlan(list, subPlan);
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
