package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.CQElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple container class to structure JSON to couple child and sample tightly.
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
	private TemporalSamplerFactory sampler;

}
