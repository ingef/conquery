package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.SampledNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class represents a wrapper around any type of {@link CQElement} but also
 * holds a sampler to select a single day from the child elements result.
 */
@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class CQSampled {
	/**
	 * A query part which should be sampled to a singe day.
	 */
	@NotNull @Valid
	private CQElement child;
	/**
	 * The sampler to be used.
	 */
	@NotNull @Valid
	private TemporalSampler sampler;
	
	/**
	 * Creates a SampleNode containing the samples and a sub query plan for this part of the query.
	 * @param ctx the context used to create the query plan
	 * @return a new SampledNode
	 */
	public SampledNode createQueryPlan(QueryPlanContext ctx) {
		ConceptQueryPlan subPlan = new ConceptQueryPlan(true);
		subPlan.setChild(child.createQueryPlan(ctx, subPlan));
		// Since we create the plan manually we have to register the lower date aggregators manually.
		// Such an aggregator exists because it was enforced in CQAbstractTemporalQuery::resolve on the child.
		subPlan.getDateAggregator().registerAll(subPlan.getChild().getDateAggregators());
		return new SampledNode(subPlan, sampler);
	}

	/**
	 * @see CQElement#resolve(QueryResolveContext)
	 */
	public void resolve(QueryResolveContext context) {

		child.resolve(context);
	}
}
