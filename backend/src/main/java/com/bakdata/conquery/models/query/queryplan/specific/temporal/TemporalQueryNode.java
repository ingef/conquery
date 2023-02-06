package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSamplerFactory;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QPParentNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import lombok.Getter;

/**
 * QueryNode implementing the logic for TemporalQueries.
 * Executes two queries and compares the times they are included, the entity is included according to a specified {@link PrecedenceMatcher}.
 */
@Getter
public class TemporalQueryNode extends QPParentNode {

	@Override
	public void init(Entity entity, QueryExecutionContext ctx) {
		super.init(entity, ctx);
		dateUnion.init(entity, ctx);

		referenceSampler = referenceSamplerFactory.sampler(ctx.getToday());
		precedingSampler = precedingSamplerFactory.sampler(ctx.getToday());
	}

	/**
	 * Matcher to be used when testing for inclusion.
	 */
	private final PrecedenceMatcher matcher;

	/**
	 * QueryPlan for the events to be compared to.
	 */
	private final QPNode reference;

	private TemporalSamplerFactory.Sampler referenceSampler;
	private final TemporalSamplerFactory referenceSamplerFactory;

	/**
	 * QueryPlan for the events being compared.
	 */
	private final QPNode preceding;

	private TemporalSamplerFactory.Sampler precedingSampler;
	private final TemporalSamplerFactory precedingSamplerFactory;


	/**
	 * The {@link SpecialDateUnion} to be fed with the included dataset.
	 */
	private final SpecialDateUnion dateUnion;

	/**
	 * We will always merge our nodes, but keep this as an option if we want to expand our functionality.
	 */
	private final DateAggregationAction dateAggregationAction = DateAggregationAction.MERGE;


	public TemporalQueryNode(QPNode reference, TemporalSamplerFactory referenceSampler, QPNode preceding, TemporalSamplerFactory precedingSampler, PrecedenceMatcher matcher, SpecialDateUnion dateUnion) {
		// We BLOCK because we are overriding the logic down below.
		super(List.of(reference, preceding), DateAggregationAction.BLOCK);

		this.reference = reference;
		this.referenceSamplerFactory = referenceSampler;

		this.preceding = preceding;
		this.precedingSamplerFactory = precedingSampler;

		this.matcher = matcher;
		this.dateUnion = dateUnion;
	}


	/**
	 * Retrieves the {@link ConceptQueryPlan#getDateAggregator()} time of {@link #reference} and {@link #preceding}.
	 * Then tests whether they match the specific criteria for inclusion.
	 * If the criteria are met, the matching {@link CDateSet} is put into the @{@link SpecialDateUnion} node of the Queries associated QueryPlan.
	 *
	 * @return true, iff the Events match the specific criteria.
	 */
	@Override
	public final boolean isContained() {
		if (!reference.isContained()) {
			return false;
		}


		CDateSet referenceDurations =
				dateAggregationAction.aggregate(getReference().getDateAggregators()
															  .stream()
															  .map(Aggregator::createAggregationResult)
															  .collect(Collectors.toSet()));

		// Create copy as we are mutating the set
		CDateSet precedingDurations =
				dateAggregationAction.aggregate(getPreceding().getDateAggregators()
															  .stream()
															  .map(Aggregator::createAggregationResult)
															  .collect(Collectors.toSet()));


		OptionalInt sampledReference = getReferenceSampler().sample(referenceDurations);

		if (sampledReference.isEmpty()) {
			return false;
		}

		matcher.removePreceding(precedingDurations, sampledReference.getAsInt());

		OptionalInt sampledPreceding = getPrecedingSampler().sample(precedingDurations);

		if (matcher.isContained(sampledReference, sampledPreceding)) {
			dateUnion.merge(referenceDurations);
			return true;
		}

		return false;
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return Set.of(dateUnion);
	}

	@Override
	public String toString() {
		return "TemporalQueryNode(" +
			   "matcher=" + matcher +
			   ", reference=" + reference +
			   ", preceding=" + preceding +
			   ", dateAggregationAction=" + dateAggregationAction +
			   ')';
	}
}
