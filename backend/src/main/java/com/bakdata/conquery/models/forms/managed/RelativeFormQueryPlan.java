package com.bakdata.conquery.models.forms.managed;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSamplerFactory;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.forms.util.DateContext;
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

	private final TemporalSamplerFactory indexSelectorFactory;
	private TemporalSamplerFactory.Sampler indexSelector;
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

		indexSelector = indexSelectorFactory.sampler();

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
		outcomeSubquery = createSubQuery(featurePlan, contexts, FeatureGroup.OUTCOME);


		featureSubquery.init(ctx, entity);
		outcomeSubquery.init(ctx, entity);

		Optional<MultilineEntityResult> featureResult = featureSubquery.execute(ctx, entity);
		Optional<MultilineEntityResult> outcomeResult = outcomeSubquery.execute(ctx, entity);

		List<Object[]> values = new ArrayList<>();

		featureResult.map(MultilineEntityResult::getValues).ifPresent(values::addAll);
		outcomeResult.map(MultilineEntityResult::getValues).ifPresent(values::addAll);

		return Optional.of(new MultilineEntityResult(entity.getId(), values));
	}

	private int getFirstAggregatorPosition() {
		if (featurePlan.getAggregatorSize() <= 0) {
			throw new ConqueryError.ExecutionProcessingError();
		}
		// We need an extra column for the observation scope
		return 5;
	}

	/**
	 * Whole result is the concatenation of the subresults. The final output format
	 * combines resolution info, index and eventdate of both sub queries. The
	 * feature/outcome sub queries are of in form of: [RESOLUTION], [INDEX], [EVENTDATE], [FEATURE/OUTCOME_DR], [FEATURE/OUTCOME_SELECTS]...
	 * The wanted format is: [RESOLUTION], [INDEX], [EVENTDATE], [FEATURE_DR], [OUTCOME_DR], [FEATURE_SELECTS]... , [OUTCOME_SELECTS]
	 */
	private int calculateCompleteLength() {

		return getFirstAggregatorPosition() + featurePlan.getAggregatorSize();
		//return featureLength + outcomeLength - 3/* ^= [RESOLUTION], [INDEX], [EVENTDATE] */;
	}

	private void assertResultWidth(EntityResult subResult, int resultWidth) {
		int resultColumnCount = subResult.columnCount();

		if (resultColumnCount == resultWidth) {
			return;
		}

		throw new IllegalStateException(String.format("Aggregator number (%d) and result number (%d) are not the same", resultWidth, resultColumnCount));
	}


	private static FormQueryPlan createSubQuery(ArrayConceptQueryPlan subPlan, List<DateContext> contexts, FeatureGroup featureGroup) {
		List<DateContext> list = new ArrayList<>(contexts);
		list.removeIf(dctx -> dctx.getFeatureGroup() != featureGroup);

		return new FormQueryPlan(list, subPlan, true);
	}

	public List<Aggregator<?>> getAggregators() {
		return ImmutableList.copyOf(featurePlan.getAggregators());
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return query.isOfInterest(entity);
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		DateAggregator agg = new DateAggregator(DateAggregationAction.MERGE);

		featureSubquery.getValidityDateAggregator().ifPresent(agg::register);
		outcomeSubquery.getValidityDateAggregator().ifPresent(agg::register);

		return agg.hasChildren() ? Optional.of(agg) : Optional.empty();
	}
}
