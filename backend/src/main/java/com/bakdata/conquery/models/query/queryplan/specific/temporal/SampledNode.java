package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.clone.CtxCloneable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class represents a wrapper around any type of {@link QPNode} but also
 * holds a sampler to select a single day from the child elements result.
 */
@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class SampledNode implements CtxCloneable<SampledNode> {

	/**
	 * A query plan which should be sampled to a singe day.
	 */
	@NotNull @Valid
	private ConceptQueryPlan child;
	/**
	 * The sampler to be used.
	 */
	@NotNull @Valid
	private TemporalSampler sampler;
	
	@Override
	public SampledNode doClone(CloneContext ctx) {
		return new SampledNode(child.clone(ctx), sampler);
	}
}
