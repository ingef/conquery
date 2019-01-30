package com.bakdata.conquery.models.concepts.temporal;

import com.bakdata.conquery.models.query.concept.CQElement;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Abstract data class specifying the data needed for a TemporalQuery
 */
@Getter
@AllArgsConstructor
public abstract class AbstractTemporalQuery implements CQElement {

	/**
	 * The query being executed as reference for preceding.
	 */
	CQElement index;

	/**
	 * The query being executed, compared to index. Events in preceding will be cut-off to be always before index, or at the same day, depending on the queries specific implementations.
	 */
	CQElement preceding;

	/**
	 * The sampler to be used.
	 */
	TemporalSampler sampler;
}
