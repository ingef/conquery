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
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter @RequiredArgsConstructor
public class RelativeFormQueryPlan implements QueryPlan {

	private final ConceptQueryPlan query;
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

		SubResult featureResult = executeSubQuery(ctx, FeatureGroup.FEATURE, entity, contexts);
		SubResult outcomeResult = executeSubQuery(ctx, FeatureGroup.OUTCOME, entity, contexts);

		List<Object[]> values = new ArrayList<>();
		// We look at the first result line to determine the length of the subresult
		int featureLength = featureResult.getValues().get(0).length;
		int outcomeLength = outcomeResult.getValues().get(0).length;
		
		/*
		 *  Whole result is the concatenation of the subresults. The final output format combines resolution info, index and eventdate of both sub queries.
		 *  The feature/outcome sub queries are of in form of: [RESOLUTION], [INDEX], [EVENTDATE], [FEATURE/OUTCOME_DR], [FEATURE/OUTCOME_SELECTS]... 
		 *  The wanted format is: [RESOLUTION], [INDEX], [EVENTDATE], [FEATURE_DR], [OUTCOME_DR], [FEATURE_SELECTS]... , [OUTCOME_SELECTS]
		 */
		int size = featureLength + outcomeLength - 3/*= [RESOLUTION], [INDEX], [EVENTDATE]*/;

		// merge the full (index == null) lines
		Object[] mergedFull = new Object[size];
		setFeatureValues(mergedFull, featureResult.getValues().get(0));
		setOutcomeValues(mergedFull, outcomeResult.getValues().get(0), featureLength);
		values.add(mergedFull);

		// append all other lines directly
		for (int i = 1; i < featureResult.getValues().size(); i++) {
			Object[] result = new Object[size];
			setFeatureValues(result, featureResult.getValues().get(i));
			values.add(result);
		}
		for (int i = 1; i < outcomeResult.getValues().size(); i++) {
			Object[] result = new Object[size];
			setOutcomeValues(result, outcomeResult.getValues().get(i), featureLength);
			values.add(result);
		}
		return EntityResult.multilineOf(entity.getId(), values);
	}

	private SubResult executeSubQuery(QueryExecutionContext ctx, FeatureGroup featureGroup, Entity entity, List<DateContext> contexts) {
		List<DateContext> list = new ArrayList<>(contexts);
		list.removeIf(dctx -> dctx.getFeatureGroup() != featureGroup);

		ArrayConceptQueryPlan subPlan = featureGroup == FeatureGroup.FEATURE ? featurePlan : outcomePlan;

		FormQueryPlan sub = new FormQueryPlan(list,subPlan);
		return new SubResult((MultilineContainedEntityResult) sub.execute(ctx, entity));
	}

	private void setFeatureValues(Object[] result, Object[] value) {
		// copy everything up to including index
		for (int i = 0; i < 3; i++) {
			result[i] = value[i];
		}
		// copy daterange
		result[3] = value[3];
		System.arraycopy(value, 4, result, 5, value.length - 4);
	}
	
	private void setOutcomeValues(Object[] result, Object[] value, int featureLength) {
		// copy everything up to including index
		for (int i = 0; i < 3; i++) {
			result[i] = value[i];
		}
		// copy daterange
		result[4] = value[3];
		System.arraycopy(value, 4, result, 1 + featureLength, value.length - 4);
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
		return query.isOfInterest(entity);
	}
	
	@AllArgsConstructor
	private static class SubResult {
		private MultilineContainedEntityResult result;
		
		public List<Object[]> getValues() {
			return result.getValues();
		}
	}
}
